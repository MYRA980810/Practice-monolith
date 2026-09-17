package com.livecomerce.cart;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published once per store, per checkout attempt, whether that store's
 * checkout succeeded or failed (R4-002 fix) — gives other modules an
 * observability hook into checkout outcomes independent of the HTTP
 * response body, since {@code CartController.checkout()} always returns
 * HTTP 200 regardless of any per-store failure (design D8). {@code
 * failureReason} and {@code orderId}/{@code total} mirror {@code
 * StoreCheckoutResult}'s corresponding fields for the store this event
 * describes.
 */
public record CartCheckoutCompletedEvent(
        UUID buyerId,
        UUID storeId,
        boolean succeeded,
        UUID orderId,
        BigDecimal total,
        String failureReason
) {}
