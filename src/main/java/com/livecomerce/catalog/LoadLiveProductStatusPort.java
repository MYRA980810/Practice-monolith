package com.livecomerce.catalog;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Cross-module port for the two live-commerce badges shown on a product card/page:
 * whether it is currently pinned in an active live ({@code pinnedNow}, cosmetic), and
 * whether it is exclusively sold through an active live right now ({@code
 * exclusiveToActiveLive}, a purchase-blocking signal consumed by the general storefront
 * cart added in a later change). Lives in {@code catalog}'s root package (its default,
 * unnamed named interface) so a module that owns live data can implement it without
 * {@code catalog} depending back on that module — same shape and rationale as
 * {@link LoadProductSalesPort} and {@link LoadProductRatingPort}, both implemented
 * outside {@code catalog}.
 */
public interface LoadLiveProductStatusPort {

    /**
     * @param pinnedNow             true if any {@code LiveProduct} for this product is
     *                              PINNED in a live currently in LIVE status.
     * @param exclusiveToActiveLive true if any {@code LiveProduct} for this product,
     *                              with status other than SOLD, belongs to a live in
     *                              LIVE or RECONNECTING status — broader than {@code
     *                              pinnedNow} since it also covers non-pinned showcase
     *                              products and the RECONNECTING grace window.
     */
    record LiveProductBadge(boolean pinnedNow, boolean exclusiveToActiveLive) {
        public static final LiveProductBadge NONE = new LiveProductBadge(false, false);
    }

    /**
     * Products absent from the result should be treated as {@link LiveProductBadge#NONE}
     * by the caller (no live association at all).
     */
    Map<UUID, LiveProductBadge> loadStatuses(Collection<UUID> productIds);
}
