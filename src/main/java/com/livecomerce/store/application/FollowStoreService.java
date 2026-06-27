package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.FollowStoreUseCase;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.StoreFollowerPort;
import com.livecomerce.store.domain.StoreFollower;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowStoreService implements FollowStoreUseCase {

    private final StoreFollowerPort storeFollowerPort;
    private final LoadStorePort loadStorePort;

    @Override
    public void follow(UUID storeId, UUID userId) {
        loadStorePort.loadById(storeId)
                .filter(s -> s.isActive())
                .orElseThrow(() -> new StoreNotFoundException(storeId.toString()));

        if (storeFollowerPort.existsFollower(storeId, userId)) {
            return;
        }

        storeFollowerPort.saveFollower(StoreFollower.create(storeId, userId));
    }
}
