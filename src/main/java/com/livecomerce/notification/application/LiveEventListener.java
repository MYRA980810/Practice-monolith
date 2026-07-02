package com.livecomerce.notification.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.LiveCancelledEvent;
import com.livecomerce.live.LiveStartedEvent;
import com.livecomerce.notification.application.port.out.SaveNotificationPort;
import com.livecomerce.notification.application.port.out.SendRtmPeerMessagePort;
import com.livecomerce.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LiveEventListener {

    private static final Logger log = LoggerFactory.getLogger(LiveEventListener.class);

    private final SaveNotificationPort   saveNotificationPort;
    private final SendRtmPeerMessagePort sendRtmPeerMessagePort;
    private final ObjectMapper           objectMapper;

    @ApplicationModuleListener
    public void on(LiveStartedEvent event) {
        if (event.subscriberIds().isEmpty()) {
            return;
        }
        var payload = Map.<String, Object>of(
                "type",    "live-started",
                "liveId",  event.liveId().toString(),
                "title",   event.title(),
                "storeId", event.storeId() != null ? event.storeId().toString() : ""
        );
        for (UUID subscriberId : event.subscriberIds()) {
            notifySubscriber(subscriberId, "live-started", event.liveId(), payload);
        }
    }

    @ApplicationModuleListener
    public void on(LiveCancelledEvent event) {
        if (event.subscriberIds().isEmpty()) {
            return;
        }
        var payload = Map.<String, Object>of(
                "type",   "live-cancelled",
                "liveId", event.liveId().toString(),
                "title",  event.title()
        );
        for (UUID subscriberId : event.subscriberIds()) {
            notifySubscriber(subscriberId, "live-cancelled", event.liveId(), payload);
        }
    }

    private void notifySubscriber(UUID subscriberId, String type, UUID liveId,
                                   Map<String, Object> payload) {
        var notification = Notification.create(subscriberId, type, liveId, payload);
        saveNotificationPort.save(notification);
        try {
            var json = objectMapper.writeValueAsString(payload);
            sendRtmPeerMessagePort.sendPeerMessage(subscriberId.toString(), json);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize RTM peer payload for subscriber {}: {}", subscriberId, e.getMessage());
        }
    }
}
