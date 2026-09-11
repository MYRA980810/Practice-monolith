package com.livecomerce.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Owned by {@code order} (not {@code review}) so that {@code order} never gains
 * an outgoing dependency on {@code review} — mirrors {@code LiveOrderFinalizePort}
 * (owned by order, called by live). {@code review} depends on {@code order} the
 * same one-way direction {@code catalog} and {@code live} already do; the reverse
 * (order depending on review) would close a cycle through catalog/live/store,
 * which already depend on order.
 */
public interface LoadOrderForReviewPort {

    record OrderLineForReview(UUID orderItemId, UUID productId) {}

    record OrderForReview(UUID orderId, UUID buyerId, UUID storeId, boolean delivered, List<OrderLineForReview> items) {}

    Optional<OrderForReview> loadForReview(UUID orderId);
}
