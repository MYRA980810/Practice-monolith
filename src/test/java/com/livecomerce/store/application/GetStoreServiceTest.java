package com.livecomerce.store.application;

import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.domain.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStoreServiceTest {

    @Mock LoadStorePort loadStorePort;

    @InjectMocks GetStoreService service;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void getByUserId_whenStoreExists_returnsStore() {
        var store = Store.create(USER_ID, "Mi Tienda", "mi-tienda", null, null);
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.of(store));

        var result = service.getByUserId(USER_ID);

        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getSlug()).isEqualTo("mi-tienda");
    }

    @Test
    void getByUserId_whenStoreNotFound_throwsStoreNotFoundException() {
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByUserId(USER_ID))
                .isInstanceOf(StoreNotFoundException.class);
    }

    @Test
    void getBySlug_whenStoreExists_returnsStore() {
        var store = Store.create(USER_ID, "Mi Tienda", "mi-tienda", null, null);
        when(loadStorePort.loadBySlug("mi-tienda")).thenReturn(Optional.of(store));

        var result = service.getBySlug("mi-tienda");

        assertThat(result.getSlug()).isEqualTo("mi-tienda");
    }

    @Test
    void getBySlug_whenStoreNotFound_throwsStoreNotFoundException() {
        when(loadStorePort.loadBySlug("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySlug("no-existe"))
                .isInstanceOf(StoreNotFoundException.class);
    }
}
