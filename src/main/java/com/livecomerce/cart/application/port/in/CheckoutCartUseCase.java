package com.livecomerce.cart.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Batches selected item ids — possibly spanning multiple stores — into at
 * most one {@code order.PlaceCartOrderPort} call per store (design D8).
 * Multi-store checkout is best-effort: one store's success stands
 * regardless of another store's failure. Per-line availability (stock,
 * live-exclusivity, and D6 fail-closed catalog-lookup failures) is checked
 * in {@code cart} itself, using the same {@code CartProductInfo} fetched for
 * hydration, BEFORE calling {@code order} — {@code order}'s batch entry
 * point cannot report which line in a batch failed with insufficient stock,
 * so {@code cart} never sends it a line it hasn't already confirmed should
 * succeed.
 */
public interface CheckoutCartUseCase {

    CheckoutCartResponse checkout(CheckoutCartCommand command);

    record CheckoutCartCommand(UUID buyerId, List<SelectedItem> selectedItems) {}

    record SelectedItem(UUID storeId, UUID productId, UUID variantId) {}

    record CheckoutCartResponse(List<StoreCheckoutResult> results) {}

    /**
     * {@code succeeded} requires at least one selected item to have been
     * placed; a store result is only ever {@code succeeded = false} when
     * zero of its selected items could be ordered (no Order is created in
     * that case), with a non-null {@code failureReason}.
     */
    record StoreCheckoutResult(
            UUID storeId,
            boolean succeeded,
            UUID orderId,
            BigDecimal total,
            String currency,
            List<SkippedLine> skippedLines,
            String failureReason
    ) {}

    /** {@code reason} is one of {@code OUT_OF_STOCK}, {@code LIVE_EXCLUSIVE}, {@code UNAVAILABLE}. */
    record SkippedLine(UUID productId, UUID variantId, String reason) {}
}
