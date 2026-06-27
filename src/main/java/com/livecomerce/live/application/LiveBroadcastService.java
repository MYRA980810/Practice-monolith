package com.livecomerce.live.application;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LiveBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastProductPinned(UUID liveId, UUID liveProductId,
                                       String productName, BigDecimal price,
                                       int displayDurationSeconds) {
        messagingTemplate.convertAndSend(
                "/topic/live/" + liveId + "/product-pinned",
                new ProductPinnedPayload(liveProductId, productName, price, displayDurationSeconds));
    }

    public void broadcastStockUpdate(UUID liveId, UUID liveProductId, int stockRemaining) {
        messagingTemplate.convertAndSend(
                "/topic/live/" + liveId + "/stock-update",
                new StockUpdatePayload(liveProductId, stockRemaining));
    }

    public void broadcastLiveEnded(UUID liveId) {
        messagingTemplate.convertAndSend(
                "/topic/live/" + liveId + "/ended",
                new LiveEndedPayload(liveId));
    }

    record ProductPinnedPayload(UUID liveProductId, String productName,
                                BigDecimal price, int displayDurationSeconds) {}
    record StockUpdatePayload(UUID liveProductId, int stockRemaining) {}
    record LiveEndedPayload(UUID liveId) {}
}
