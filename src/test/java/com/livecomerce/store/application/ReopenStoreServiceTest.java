package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.ReopenStoreUseCase;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.SaveStorePort;
import com.livecomerce.store.domain.Store;
import com.livecomerce.store.domain.SuspensionReason;
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
class ReopenStoreServiceTest {

    @Mock LoadStorePort loadStorePort;
    @Mock SaveStorePort saveStorePort;

    @InjectMocks ReopenStoreService service;

    private static final UUID USER_ID = UUID.randomUUID();

    private Store closedStore() {
        var store = Store.create(USER_ID, "Mi Tienda", "mi-tienda", null, null);
        store.closeTemporarily();
        return store;
    }

    @Test
    void reopen_whenClosed_reopensAndSaves() {
        var store = closedStore();
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.of(store));

        service.reopen(USER_ID);

        ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
        verify(saveStorePort).save(captor.capture());
        assertThat(captor.getValue().isTemporarilyClosed()).isFalse();
    }

    @Test
    void reopen_whenStoreNotFound_throwsStoreNotFoundException() {
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reopen(USER_ID))
                .isInstanceOf(StoreNotFoundException.class);
        verify(saveStorePort, never()).save(any());
    }

    @Test
    void reopen_whenSuspended_throwsStoreCannotBeReopenedException() {
        var store = Store.create(USER_ID, "Mi Tienda", "mi-tienda", null, null);
        store.suspend(SuspensionReason.BILLING);
        store.closeTemporarily();
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> service.reopen(USER_ID))
                .isInstanceOf(StoreCannotBeReopenedException.class);
        verify(saveStorePort, never()).save(any());
    }
}
