package com.livecomerce.cart.application.port.in;

import java.util.UUID;

/**
 * Adds a line to a buyer's per-store cart. Live-exclusive products are hard
 * rejected (design D4); a line whose price/stock or live-exclusivity signal
 * could not be resolved is treated as unavailable, not silently allowed
 * (design D6).
 */
public interface AddToCartUseCase {

    AddToCartResult addToCart(AddToCartCommand command);

    record AddToCartCommand(UUID buyerId, UUID storeId, UUID productId, UUID variantId, int quantity) {}

    /**
     * {@code rejectionReason} is {@code null} on success, or one of
     * {@code LIVE_EXCLUSIVE} / {@code UNAVAILABLE} / {@code
     * QUANTITY_LIMIT_EXCEEDED} on failure.
     */
    record AddToCartResult(boolean success, String rejectionReason) {

        public static AddToCartResult accepted() {
            return new AddToCartResult(true, null);
        }

        public static AddToCartResult rejected(String reason) {
            return new AddToCartResult(false, reason);
        }
    }
}
