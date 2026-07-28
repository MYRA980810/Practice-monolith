package com.livecomerce.live.infrastructure.ivs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.out.LoadLivePort;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Consumes Amazon IVS stream-lifecycle events ("IVS Stream State Change") delivered by
 * EventBridge onto the {@code ivs-stream-events} SQS queue.
 *
 * <p>This is deliberately a thin, log-only observability hook: it correlates the event's
 * channel ARN ({@code resources[0]}) against {@code Live.ivsChannelArn} and logs the outcome.
 * It never mutates {@code Live} state — in particular it does NOT call {@code live.end()} on
 * "Stream End"/"Session Ended", since a brief reconnect would otherwise look like a false end.
 * The authoritative end of a live remains the seller's explicit action.
 */
@Component
@RequiredArgsConstructor
class IvsStreamEventListener {

    private static final Logger log = LoggerFactory.getLogger(IvsStreamEventListener.class);

    private final LoadLivePort loadLivePort;
    private final ObjectMapper objectMapper;

    @SqsListener("${ivs.stream-events-queue-name}")
    void onStreamStateEvent(String rawMessage) {
        try {
            var event = objectMapper.readValue(rawMessage, IvsEventBridgeEvent.class);
            var detail = event.detail();
            var channelArn = event.resources() != null && !event.resources().isEmpty()
                    ? event.resources().get(0) : null;

            if (channelArn == null) {
                log.warn("IVS EventBridge event missing channel ARN in resources[]: eventName={}",
                        detail != null ? detail.eventName() : null);
                return;
            }

            var liveOpt = loadLivePort.loadByIvsChannelArn(channelArn);
            if (liveOpt.isEmpty()) {
                log.warn("IVS EventBridge event for unknown channelArn={}, eventName={}",
                        channelArn, detail != null ? detail.eventName() : null);
                return;
            }

            var live = liveOpt.get();
            String eventName = detail != null && detail.eventName() != null
                    ? detail.eventName() : "unknown";

            switch (eventName) {
                case "Stream Start" ->
                        log.info("IVS stream started: liveId={}, channelArn={}, streamId={}",
                                live.getId(), channelArn, detail.streamId());
                case "Stream End", "Session Ended" ->
                        log.info("IVS stream ended (informational, not auto-ending live): liveId={}, channelArn={}, streamId={}",
                                live.getId(), channelArn, detail.streamId());
                case "Stream Failure", "Stream Takeover Failure" ->
                        log.warn("IVS stream failure event: liveId={}, channelArn={}, eventName={}, code={}",
                                live.getId(), channelArn, eventName, detail.code());
                default ->
                        log.info("IVS EventBridge event received: liveId={}, channelArn={}, eventName={}",
                                live.getId(), channelArn, eventName);
            }
        } catch (Exception e) {
            log.warn("Failed to process IVS EventBridge message: {}", e.getMessage());
        }
    }
}
