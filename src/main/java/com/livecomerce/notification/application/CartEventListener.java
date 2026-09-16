package com.livecomerce.notification.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.cart.CartItemDepletedEvent;
import com.livecomerce.notification.application.port.out.SaveNotificationPort;
import com.livecomerce.notification.application.port.out.SendRtmPeerMessagePort;
import com.livecomerce.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles {@link CartItemDepletedEvent} (design D9), same shape as {@link
 * LiveEventListener}. {@code liveId} stays {@code null} on the persisted
 * {@link Notification} — {@code productId} travels in the JSON payload
 * instead, avoiding any schema migration.
 */
@Component
@RequiredArgsConstructor
public class CartEventListener {

    private static final Logger log = LoggerFactory.getLogger(CartEventListener.class);

    private final SaveNotificationPort   saveNotificationPort;
    private final SendRtmPeerMessagePort sendRtmPeerMessagePort;
    private final ObjectMapper           objectMapper;

    @ApplicationModuleListener
    public void on(CartItemDepletedEvent event) {
        var payload = Map.<String, Object>of(
                "type",           "cart-item-depleted",
                "storeId",        event.storeId().toString(),
                "productId",      event.productId().toString(),
                "variantId",      event.variantId() != null ? event.variantId().toString() : "",
                "productName",    event.productName(),
                "threshold",      event.threshold(),
                "availableStock", event.availableStock()
        );

        var notification = Notification.create(event.buyerId(), "cart-item-depleted", null, payload);
        saveNotificationPort.save(notification);
        try {
            var json = objectMapper.writeValueAsString(payload);
            sendRtmPeerMessagePort.sendPeerMessage(event.buyerId().toString(), json);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize RTM peer payload for buyer {}: {}", event.buyerId(), e.getMessage());
        }
    }
}
