package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.UnfollowStoreUseCase;
import com.livecomerce.store.application.port.out.StoreFollowerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UnfollowStoreService implements UnfollowStoreUseCase {

    private final StoreFollowerPort storeFollowerPort;

    @Override
    public void unfollow(UUID storeId, UUID userId) {
        storeFollowerPort.deleteFollower(storeId, userId);
    }
}
