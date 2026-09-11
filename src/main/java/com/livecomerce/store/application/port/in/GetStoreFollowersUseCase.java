package com.livecomerce.store.application.port.in;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface GetStoreFollowersUseCase {

    long getFollowerCount(UUID storeId);

    boolean isFollowing(UUID storeId, UUID userId);

    /** Batch lookup for cross-module callers (e.g. the ranking job in {@code analytics}). */
    Map<UUID, Long> getFollowerCounts(Collection<UUID> storeIds);
}
