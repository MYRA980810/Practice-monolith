package com.livecomerce.store.application.port.out;

import com.livecomerce.store.domain.StoreFollower;

import java.util.List;
import java.util.UUID;

public interface StoreFollowerPort {

    void saveFollower(StoreFollower follower);

    void deleteFollower(UUID storeId, UUID userId);

    boolean existsFollower(UUID storeId, UUID userId);

    long countFollowers(UUID storeId);

    List<UUID> findFollowedStoreIds(UUID userId);
}
