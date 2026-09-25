package com.livecomerce.store;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Category lookups the store module needs from catalog. Lives in store's root package so
 * catalog (which already depends on store) implements it without creating a module cycle.
 * Batch methods return an empty map for empty input without querying.
 */
public interface StoreCategoryPort {

    record CategoryRef(UUID id, String name, String slug) {}

    /**
     * Dominant ACTIVE category per store, by count of active, non-paused (buyer-visible)
     * products. Ties resolve to the lowest category UUID so the result is deterministic.
     * Stores with no categorized buyer-visible products (or only inactive categories) are
     * absent from the map.
     */
    Map<UUID, CategoryRef> inferTopByStore(Collection<UUID> storeIds);

    /** Categories among {@code ids} that exist and are ACTIVE, keyed by category id. */
    Map<UUID, CategoryRef> loadActiveByIds(Collection<UUID> ids);

    boolean isActive(UUID categoryId);
}
