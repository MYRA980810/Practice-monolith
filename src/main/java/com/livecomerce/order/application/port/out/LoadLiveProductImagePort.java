package com.livecomerce.order.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Batched read of {@code live_products.image_url} for a live's products.
 * Nullable (never captured) with no fallback join to catalog — an absent
 * key simply means the item's imageUrl is null.
 */
public interface LoadLiveProductImagePort {

    Map<UUID, String> findImageUrls(UUID liveId, Collection<UUID> productIds);
}
