package com.livecomerce.cart;

import java.util.UUID;

/**
 * Published by {@code CartStockDepletionService} whenever a cart line's
 * available stock crosses a new, more severe threshold (design D9). {@code
 * threshold} is the crossed threshold's value (e.g. {@code 5} for "low
 * stock", {@code 0} for "out of stock") — consumers use it purely for the
 * notification copy, dedupe itself lives in {@code CartStorePort}'s
 * notified-threshold state.
 */
public record CartItemDepletedEvent(
        UUID buyerId,
        UUID storeId,
        UUID productId,
        UUID variantId,
        String productName,
        int threshold,
        int availableStock
) {}
