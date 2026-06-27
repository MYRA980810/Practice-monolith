package com.livecomerce.store.application;

import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.StoreFollowerPort;
import com.livecomerce.store.domain.Store;
import com.livecomerce.store.domain.StoreFollower;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowStoreServiceTest {

    @Mock StoreFollowerPort storeFollowerPort;
    @Mock LoadStorePort loadStorePort;

    @InjectMocks FollowStoreService service;

    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void follow_whenStoreExistsAndNotFollowing_savesFollower() {
        var store = Store.create(UUID.randomUUID(), "Tienda", "tienda", null, null);
        when(loadStorePort.loadById(STORE_ID)).thenReturn(Optional.of(store));
        when(storeFollowerPort.existsFollower(STORE_ID, USER_ID)).thenReturn(false);

        service.follow(STORE_ID, USER_ID);

        var captor = ArgumentCaptor.forClass(StoreFollower.class);
        verify(storeFollowerPort).saveFollower(captor.capture());
        assertThat(captor.getValue().getStoreId()).isEqualTo(STORE_ID);
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void follow_whenAlreadyFollowing_doesNotSaveAgain() {
        var store = Store.create(UUID.randomUUID(), "Tienda", "tienda", null, null);
        when(loadStorePort.loadById(STORE_ID)).thenReturn(Optional.of(store));
        when(storeFollowerPort.existsFollower(STORE_ID, USER_ID)).thenReturn(true);

        service.follow(STORE_ID, USER_ID);

        verify(storeFollowerPort, never()).saveFollower(any());
    }

    @Test
    void follow_whenStoreNotFound_throwsStoreNotFoundException() {
        when(loadStorePort.loadById(STORE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.follow(STORE_ID, USER_ID))
                .isInstanceOf(StoreNotFoundException.class);

        verify(storeFollowerPort, never()).saveFollower(any());
    }

    @Test
    void follow_whenStoreInactive_throwsStoreNotFoundException() {
        var store = Store.create(UUID.randomUUID(), "Tienda", "tienda", null, null);
        store.deactivate();
        when(loadStorePort.loadById(STORE_ID)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> service.follow(STORE_ID, USER_ID))
                .isInstanceOf(StoreNotFoundException.class);

        verify(storeFollowerPort, never()).saveFollower(any());
    }
}
