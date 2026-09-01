package com.livecomerce.live.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.LiveEndedEvent;
import com.livecomerce.live.LiveStartedEvent;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * On {@link LiveStartedEvent} / {@link LiveEndedEvent}, pushes a lightweight
 * add/remove signal to a global Agora RTM channel so the buyer-facing lives
 * feed can update in real time without polling. Payload is intentionally
 * minimal ({@code type} + {@code liveId}) — the client re-fetches the full
 * card via the existing REST feed endpoints rather than duplicating seller-
 * name/thumbnail enrichment here.
 * <p>
 * Tolerant of Modulith's at-least-once event redelivery: re-sending the same
 * signal twice is a no-op for a client that indexes cards by liveId.
 */
@Component
@RequiredArgsConstructor
public class LiveFeedBroadcastListener {

    private static final Logger log = LoggerFactory.getLogger(LiveFeedBroadcastListener.class);

    static final String FEED_CHANNEL = "lives-feed";

    private final AgoraRtmMessagePort agoraRtmMessagePort;
    private final ObjectMapper        objectMapper;

    @ApplicationModuleListener
    public void on(LiveStartedEvent event) {
        broadcast(Map.of("type", "live-started", "liveId", event.liveId().toString()));
    }

    @ApplicationModuleListener
    public void on(LiveEndedEvent event) {
        broadcast(Map.of("type", "live-ended", "liveId", event.liveId().toString()));
    }

    private void broadcast(Map<String, Object> payload) {
        try {
            agoraRtmMessagePort.sendChannelMessage(FEED_CHANNEL, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("Agora RTM lives-feed broadcast failed: {}", e.getMessage());
        }
    }
}
