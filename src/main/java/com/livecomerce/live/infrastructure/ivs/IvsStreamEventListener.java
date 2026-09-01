package com.livecomerce.live.infrastructure.ivs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Consumes Amazon IVS stream-lifecycle events ("IVS Stream State Change") delivered by
 * EventBridge onto the {@code ivs-stream-events} SQS queue.
 *
 * <p>It correlates the event's channel ARN ({@code resources[0]}) against
 * {@code Live.ivsChannelArn}. It deliberately does NOT call {@code live.end()} directly on
 * "Stream End"/"Session Ended" — a brief reconnect would otherwise look like a false end.
 * Instead it records the disconnect timestamp via {@code live.markStreamEnded(...)}; the
 * stale-live reconciliation job is what actually ends a live whose signal outlives the grace
 * period. A subsequent "Stream Start" clears the signal via {@code live.clearStreamEndedSignal()}.
 * The seller's explicit end action remains unaffected either way.
 */
@Component
@RequiredArgsConstructor
class IvsStreamEventListener {

    private static final Logger log = LoggerFactory.getLogger(IvsStreamEventListener.class);

    private final LoadLivePort loadLivePort;
    private final SaveLivePort saveLivePort;
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

            var liveOpt = loadLivePort.loadActiveByIvsChannelArn(channelArn);
            if (liveOpt.isEmpty()) {
                log.warn("IVS EventBridge event for unknown channelArn={}, eventName={}",
                        channelArn, detail != null ? detail.eventName() : null);
                return;
            }

            var live = liveOpt.get();
            String eventName = detail != null && detail.eventName() != null
                    ? detail.eventName() : "unknown";
            String streamId = detail != null ? detail.streamId() : null;
            String code = detail != null ? detail.code() : null;

            switch (eventName) {
                case "Stream Start" -> {
                    live.clearStreamEndedSignal();
                    saveLivePort.save(live);
                    log.info("IVS stream started: liveId={}, channelArn={}, streamId={}",
                            live.getId(), channelArn, streamId);
                }
                case "Stream End", "Session Ended" -> {
                    live.markStreamEnded(Instant.now());
                    saveLivePort.save(live);
                    log.info("IVS stream ended, grace period started: liveId={}, channelArn={}, streamId={}",
                            live.getId(), channelArn, streamId);
                }
                case "Stream Failure", "Stream Takeover Failure" ->
                        log.warn("IVS stream failure event: liveId={}, channelArn={}, eventName={}, code={}",
                                live.getId(), channelArn, eventName, code);
                default ->
                        log.info("IVS EventBridge event received: liveId={}, channelArn={}, eventName={}",
                                live.getId(), channelArn, eventName);
            }
        } catch (Exception e) {
            log.warn("Failed to process IVS EventBridge message: {}", e.getMessage());
        }
    }
}
