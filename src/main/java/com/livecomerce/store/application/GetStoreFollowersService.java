package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.GetStoreFollowersUseCase;
import com.livecomerce.store.application.port.out.StoreFollowerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetStoreFollowersService implements GetStoreFollowersUseCase {

    private final StoreFollowerPort storeFollowerPort;

    @Override
    public long getFollowerCount(UUID storeId) {
        return storeFollowerPort.countFollowers(storeId);
    }

    @Override
    public boolean isFollowing(UUID storeId, UUID userId) {
        return storeFollowerPort.existsFollower(storeId, userId);
    }
}
