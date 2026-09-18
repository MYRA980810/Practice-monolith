package com.livecomerce.cart.application.port.in;

import java.util.UUID;

/**
 * Increments or decrements an existing cart line's quantity by {@code
 * delta}. An increment that would exceed currently available stock is
 * blocked entirely — no partial apply (spec-confirmed default). A decrement
 * to zero or below removes the line (design D7).
 */
public interface ChangeQuantityUseCase {

    ChangeQuantityResult changeQuantity(ChangeQuantityCommand command);

    record ChangeQuantityCommand(UUID buyerId, UUID storeId, UUID productId, UUID variantId, int delta) {}

    /**
     * {@code failureReason} is {@code null} on success, or one of
     * {@code LINE_NOT_FOUND} / {@code INSUFFICIENT_STOCK} / {@code
     * UNAVAILABLE} / {@code QUANTITY_LIMIT_EXCEEDED} on failure. {@code
     * availableStock} is only populated for {@code INSUFFICIENT_STOCK}.
     * {@code resultingQuantity} is {@code 0} when the line was removed.
     */
    record ChangeQuantityResult(boolean success, String failureReason, Integer availableStock, int resultingQuantity) {

        public static ChangeQuantityResult success(int resultingQuantity) {
            return new ChangeQuantityResult(true, null, null, resultingQuantity);
        }

        public static ChangeQuantityResult insufficientStock(int availableStock) {
            return new ChangeQuantityResult(false, "INSUFFICIENT_STOCK", availableStock, 0);
        }

        public static ChangeQuantityResult quantityLimitExceeded() {
            return new ChangeQuantityResult(false, "QUANTITY_LIMIT_EXCEEDED", null, 0);
        }

        public static ChangeQuantityResult failure(String reason) {
            return new ChangeQuantityResult(false, reason, null, 0);
        }
    }
}
