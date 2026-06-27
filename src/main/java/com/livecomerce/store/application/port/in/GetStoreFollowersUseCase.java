package com.livecomerce.store.application.port.in;

import java.util.UUID;

public interface GetStoreFollowersUseCase {

    long getFollowerCount(UUID storeId);

    boolean isFollowing(UUID storeId, UUID userId);
}
