package com.livecomerce.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Cross-module port letting {@code cart} place a single batched Order per
 * store checkout. Deliberately carries no {@code liveSessionId} field —
 * {@code PlaceCartOrderService} hardcodes {@code Order.open(buyerId,
 * storeId, null, currency)} — and no {@code OrderItemType}, so {@code cart}
 * never needs to depend on {@code order}'s domain package (only this root
 * package, per {@code cart}'s {@code allowedDependencies}). The internal
 * {@code order.application.PlaceCartOrderAdapter} maps each {@link
 * CartOrderLine} to a regular PRODUCT line item.
 *
 * <p>Same "root-package interface implemented by a package-private
 * application-layer adapter in the owning module" shape as {@link
 * LivePurchasePort} / {@code LivePurchaseAdapter}.
 */
public interface PlaceCartOrderPort {

    record CartOrderLine(
            UUID productId,
            UUID variantId,
            String productName,
            BigDecimal unitPrice,
            int quantity
    ) {}

    record PlaceCartOrderCommand(
            UUID buyerId,
            UUID storeId,
            String currency,
            List<CartOrderLine> lines
    ) {}

    /**
     * Self-contained result — no {@code order.domain.Order} exposure — so
     * {@code cart} never needs {@code order::domain}.
     */
    record PlacedOrder(UUID orderId, BigDecimal total, String currency) {}

    PlacedOrder placeOrder(PlaceCartOrderCommand command);
}
