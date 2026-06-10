package com.livecomerce.store.application;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactivateStoreServiceTest {

    @Mock LoadStorePort loadStorePort;
    @Mock SaveStorePort saveStorePort;

    @InjectMocks ReactivateStoreService service;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void reactivate_whenStoreExistsAndNotSuspended_setsActiveAndSaves() {
        var store = Store.create(USER_ID, "Mi Tienda", "mi-tienda", null, null);
        store.deactivate();
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.of(store));

        service.reactivate(USER_ID);

        var captor = ArgumentCaptor.forClass(Store.class);
        verify(saveStorePort).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(captor.getValue().isSuspended()).isFalse();
    }

    @Test
    void reactivate_whenStoreIsSuspended_throwsAndDoesNotSave() {
        var store = Store.create(USER_ID, "Mi Tienda", "mi-tienda", null, null);
        store.suspend(SuspensionReason.BILLING);
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> service.reactivate(USER_ID))
                .isInstanceOf(StoreCannotBeReactivatedException.class);

        verify(saveStorePort, never()).save(any());
    }

    @Test
    void reactivate_whenStoreNotFound_throwsStoreNotFoundException() {
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reactivate(USER_ID))
                .isInstanceOf(StoreNotFoundException.class);

        verify(saveStorePort, never()).save(any());
    }
}
