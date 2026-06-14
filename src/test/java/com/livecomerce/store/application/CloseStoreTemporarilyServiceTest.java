package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.CloseStoreTemporarilyUseCase;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.SaveStorePort;
import com.livecomerce.store.domain.Store;
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
class CloseStoreTemporarilyServiceTest {

    @Mock LoadStorePort loadStorePort;
    @Mock SaveStorePort saveStorePort;

    @InjectMocks CloseStoreTemporarilyService service;

    private static final UUID USER_ID = UUID.randomUUID();

    private Store activeStore() {
        return Store.create(USER_ID, "Mi Tienda", "mi-tienda", null, null);
    }

    @Test
    void close_whenActive_closesAndSaves() {
        var store = activeStore();
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.of(store));

        service.close(USER_ID);

        ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
        verify(saveStorePort).save(captor.capture());
        assertThat(captor.getValue().isTemporarilyClosed()).isTrue();
    }

    @Test
    void close_whenStoreNotFound_throwsStoreNotFoundException() {
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.close(USER_ID))
                .isInstanceOf(StoreNotFoundException.class);
        verify(saveStorePort, never()).save(any());
    }
}
