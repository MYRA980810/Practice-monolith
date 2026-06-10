package com.livecomerce.store.application;

import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.domain.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class ListStoresServiceTest {

    @Mock LoadStorePort loadStorePort;

    @InjectMocks ListStoresService service;

    @Test
    void listActive_returnsPaginatedActiveStores() {
        var pageable = PageRequest.of(0, 20);
        var store = Store.create(UUID.randomUUID(), "Tienda A", "tienda-a", null, null);
        var page = new PageImpl<>(List.of(store), pageable, 1);
        when(loadStorePort.loadAllActive(pageable)).thenReturn(page);

        var result = service.listActive(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getSlug()).isEqualTo("tienda-a");
    }

    @Test
    void listActive_whenNoStores_returnsEmptyPage() {
        var pageable = PageRequest.of(0, 20);
        when(loadStorePort.loadAllActive(pageable)).thenReturn(new PageImpl<>(List.of()));

        var result = service.listActive(pageable);

        assertThat(result.isEmpty()).isTrue();
    }
}
