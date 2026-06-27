package com.livecomerce.store.application.port.in;

import java.util.UUID;

public interface FollowStoreUseCase {

    void follow(UUID storeId, UUID userId);
}
