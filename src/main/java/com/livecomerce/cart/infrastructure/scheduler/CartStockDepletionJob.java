package com.livecomerce.cart.infrastructure.scheduler;

import com.livecomerce.cart.application.CartStockDepletionService;
import com.livecomerce.cart.application.port.out.CartStorePort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sweeps every buyer with an active cart for stock-depletion crossings
 * (design D9), same shape as {@code StaleLiveReconciliationJob}: not itself
 * {@code @Transactional}, per-buyer try/catch so one buyer's failure never
 * blocks the rest of the tick. Enumerates buyers via {@link
 * CartStorePort#scanAllBuyerIds()}, which MUST use a non-blocking Redis
 * {@code SCAN} in production — never a blocking {@code KEYS}.
 */
@Component
@RequiredArgsConstructor
class CartStockDepletionJob {

    private static final Logger log = LoggerFactory.getLogger(CartStockDepletionJob.class);

    private final CartStorePort cartStorePort;
    private final CartStockDepletionService cartStockDepletionService;

    @Scheduled(fixedDelayString = "${cart.stock-check-ms:300000}")
    void checkDepletion() {
        var buyerIds = cartStorePort.scanAllBuyerIds();
        if (buyerIds.isEmpty()) {
            return;
        }

        for (var buyerId : buyerIds) {
            try {
                cartStockDepletionService.checkBuyerCarts(buyerId);
            } catch (Exception e) {
                log.warn("Failed to check cart stock depletion for buyer {}: {}", buyerId, e.getMessage());
            }
        }
    }
}
