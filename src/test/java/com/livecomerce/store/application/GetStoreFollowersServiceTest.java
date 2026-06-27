package com.livecomerce.store.application;

import com.livecomerce.store.application.port.out.StoreFollowerPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStoreFollowersServiceTest {

    @Mock StoreFollowerPort storeFollowerPort;

    @InjectMocks GetStoreFollowersService service;

    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void getFollowerCount_returnsCountFromPort() {
        when(storeFollowerPort.countFollowers(STORE_ID)).thenReturn(42L);

        long count = service.getFollowerCount(STORE_ID);

        assertThat(count).isEqualTo(42L);
    }

    @Test
    void getFollowerCount_whenNoFollowers_returnsZero() {
        when(storeFollowerPort.countFollowers(STORE_ID)).thenReturn(0L);

        long count = service.getFollowerCount(STORE_ID);

        assertThat(count).isEqualTo(0L);
    }

    @Test
    void isFollowing_whenFollowing_returnsTrue() {
        when(storeFollowerPort.existsFollower(STORE_ID, USER_ID)).thenReturn(true);

        boolean result = service.isFollowing(STORE_ID, USER_ID);

        assertThat(result).isTrue();
    }

    @Test
    void isFollowing_whenNotFollowing_returnsFalse() {
        when(storeFollowerPort.existsFollower(STORE_ID, USER_ID)).thenReturn(false);

        boolean result = service.isFollowing(STORE_ID, USER_ID);

        assertThat(result).isFalse();
    }
}
