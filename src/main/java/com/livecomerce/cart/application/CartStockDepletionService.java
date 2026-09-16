package com.livecomerce.cart.application;

import com.livecomerce.cart.CartItemDepletedEvent;
import com.livecomerce.cart.application.port.out.CartStorePort;
import com.livecomerce.cart.domain.Cart;
import com.livecomerce.cart.domain.CartItem;
import com.livecomerce.catalog.LoadCartProductInfoPort;
import com.livecomerce.catalog.LoadCartProductInfoPort.CartLineRef;
import com.livecomerce.catalog.LoadCartProductInfoPort.CartProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sweeps a buyer's carts for lines whose available stock has crossed a new,
 * more severe threshold since the last sweep, and publishes {@link
 * CartItemDepletedEvent} exactly once per distinct crossing (design D9,
 * spec "Threshold-crossing notification, once per crossing"). Dedupe state
 * (the last threshold notified per line) lives in {@link CartStorePort},
 * TTL-matched to the cart itself.
 *
 * <p>Thresholds are deliberately two fixed severity levels — {@code
 * LOW_STOCK_THRESHOLD} and {@code OUT_OF_STOCK_THRESHOLD} — not specified by
 * exact value in the spec/design; chosen to match the spec's own
 * illustrative example (10→4 crosses "low stock", further to 0 crosses "out
 * of stock").
 */
@Service
@RequiredArgsConstructor
public class CartStockDepletionService {

    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int OUT_OF_STOCK_THRESHOLD = 0;

    private final CartStorePort cartStorePort;
    private final LoadCartProductInfoPort loadCartProductInfoPort;
    private final ApplicationEventPublisher eventPublisher;

    public void checkBuyerCarts(UUID buyerId) {
        for (UUID storeId : cartStorePort.loadStoreIds(buyerId)) {
            checkStoreCart(buyerId, storeId);
        }
    }

    private void checkStoreCart(UUID buyerId, UUID storeId) {
        Cart cart = cartStorePort.load(buyerId, storeId);
        if (cart.isEmpty()) {
            return;
        }

        Set<CartLineRef> refs = new LinkedHashSet<>();
        for (CartItem item : cart.items()) {
            refs.add(new CartLineRef(item.key().productId(), item.key().variantId()));
        }
        Map<CartLineRef, CartProductInfo> infoByRef = loadCartProductInfoPort.loadForCart(refs);

        for (CartItem item : cart.items()) {
            var ref = new CartLineRef(item.key().productId(), item.key().variantId());
            var info = infoByRef.get(ref);
            if (info == null) {
                // Fail-safe, not fail-closed: a sweep that can't resolve a
                // line's stock simply skips it this tick rather than firing
                // a possibly-wrong notification.
                continue;
            }
            checkLine(buyerId, storeId, item.key().productId(), item.key().variantId(), info);
        }
    }

    private void checkLine(UUID buyerId, UUID storeId, UUID productId, UUID variantId, CartProductInfo info) {
        Integer crossedThreshold = highestSeverityCrossed(info.availableStock());
        Integer lastNotified = cartStorePort.loadNotifiedThreshold(buyerId, storeId, productId, variantId);

        if (crossedThreshold == null) {
            // Stock has recovered above every configured threshold
            // (JDA2-005/JDB2-002): clear any recorded dedupe state so a
            // later re-crossing of the SAME threshold fires a fresh
            // notification instead of being permanently suppressed.
            if (lastNotified != null) {
                cartStorePort.clearNotifiedThreshold(buyerId, storeId, productId, variantId);
            }
            return;
        }

        if (lastNotified != null && crossedThreshold >= lastNotified) {
            return;
        }

        cartStorePort.recordNotifiedThreshold(buyerId, storeId, productId, variantId, crossedThreshold);
        eventPublisher.publishEvent(new CartItemDepletedEvent(
                buyerId, storeId, productId, variantId, info.name(), crossedThreshold, info.availableStock()));
    }

    /**
     * @return the most severe (lowest) threshold value that {@code
     * availableStock} has crossed, or {@code null} if it hasn't crossed any.
     */
    private static Integer highestSeverityCrossed(int availableStock) {
        if (availableStock <= OUT_OF_STOCK_THRESHOLD) {
            return OUT_OF_STOCK_THRESHOLD;
        }
        if (availableStock <= LOW_STOCK_THRESHOLD) {
            return LOW_STOCK_THRESHOLD;
        }
        return null;
    }
}
