package com.livecomerce.catalog;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Cross-module port for the "sold count" badge shown on product cards/pages.
 * Lives in {@code catalog}'s root package (its default, unnamed named
 * interface) so a module that owns real order data can implement it without
 * {@code catalog} depending back on that module — same shape and rationale as
 * {@link LoadProductRatingPort}, implemented by {@code review}.
 */
public interface LoadProductSalesPort {

    /**
     * Cumulative paid units sold per product, all-time. Products absent from
     * the result (e.g. never sold) should be treated as zero by the caller.
     */
    Map<UUID, Long> loadSoldCounts(Collection<UUID> productIds);
}
