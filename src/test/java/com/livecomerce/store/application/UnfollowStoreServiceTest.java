package com.livecomerce.store.application;

import com.livecomerce.store.application.port.out.StoreFollowerPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UnfollowStoreServiceTest {

    @Mock StoreFollowerPort storeFollowerPort;

    @InjectMocks UnfollowStoreService service;

    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void unfollow_delegatesToPort() {
        service.unfollow(STORE_ID, USER_ID);

        verify(storeFollowerPort).deleteFollower(STORE_ID, USER_ID);
    }

    @Test
    void unfollow_withDifferentIds_delegatesCorrectly() {
        var otherStoreId = UUID.randomUUID();
        var otherUserId = UUID.randomUUID();

        service.unfollow(otherStoreId, otherUserId);

        verify(storeFollowerPort).deleteFollower(otherStoreId, otherUserId);
    }
}
