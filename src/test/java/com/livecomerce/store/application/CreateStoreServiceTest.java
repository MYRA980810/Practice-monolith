package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.CreateStoreUseCase.CreateStoreCommand;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.SaveStorePort;
import com.livecomerce.store.domain.Store;
import com.livecomerce.store.domain.StoreCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateStoreServiceTest {

    @Mock LoadStorePort loadStorePort;
    @Mock SaveStorePort saveStorePort;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks CreateStoreService service;

    private static final UUID USER_ID = UUID.randomUUID();

    private static final CreateStoreCommand VALID_COMMAND = new CreateStoreCommand(
            USER_ID, "Mi Tienda", "mi-tienda", "Descripción de prueba", null
    );

    @Test
    void create_withNewUserAndSlug_savesStoreAndReturnsIt() {
        var saved = Store.create(USER_ID, "Mi Tienda", "mi-tienda", "Descripción", null);
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.empty());
        when(loadStorePort.loadBySlug("mi-tienda")).thenReturn(Optional.empty());
        when(saveStorePort.save(any())).thenReturn(saved);

        var result = service.create(VALID_COMMAND);

        assertThat(result.getName()).isEqualTo("Mi Tienda");
        assertThat(result.getSlug()).isEqualTo("mi-tienda");
        assertThat(result.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void create_whenUserAlreadyHasStore_throwsStoreAlreadyExists() {
        var existing = Store.create(USER_ID, "Otra", "otra", null, null);
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(VALID_COMMAND))
                .isInstanceOf(StoreAlreadyExistsException.class);

        verify(saveStorePort, never()).save(any());
    }

    @Test
    void create_whenSlugAlreadyTaken_throwsSlugAlreadyTaken() {
        when(loadStorePort.loadByUserId(USER_ID)).thenReturn(Optional.empty());
        var other = Store.create(UUID.randomUUID(), "Otra", "mi-tienda", null, null);
        when(loadStorePort.loadBySlug("mi-tienda")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.create(VALID_COMMAND))
                .isInstanceOf(SlugAlreadyTakenException.class);

        verify(saveStorePort, never()).save(any());
    }

    @Test
    @SuppressWarnings("null")
    void create_publishesStoreCreatedEvent() {
        var saved = Store.create(USER_ID, "Mi Tienda", "mi-tienda", null, null);
        when(loadStorePort.loadByUserId(any())).thenReturn(Optional.empty());
        when(loadStorePort.loadBySlug(any())).thenReturn(Optional.empty());
        when(saveStorePort.save(any())).thenReturn(saved);

        service.create(VALID_COMMAND);

        var captor = ArgumentCaptor.forClass(StoreCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().slug()).isEqualTo("mi-tienda");
    }

    @Test
    void create_newStoreHasFreePlan() {
        var saved = Store.create(USER_ID, "Mi Tienda", "mi-tienda", null, null);
        when(loadStorePort.loadByUserId(any())).thenReturn(Optional.empty());
        when(loadStorePort.loadBySlug(any())).thenReturn(Optional.empty());
        when(saveStorePort.save(any())).thenReturn(saved);

        service.create(VALID_COMMAND);

        var captor = ArgumentCaptor.forClass(Store.class);
        verify(saveStorePort).save(captor.capture());
        assertThat(captor.getValue().getPlan().getCode()).isEqualTo("FREE");
    }
}
