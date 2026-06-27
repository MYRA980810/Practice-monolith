package com.livecomerce.store.application.port.in;

import java.util.UUID;

public interface UnfollowStoreUseCase {

    void unfollow(UUID storeId, UUID userId);
}
