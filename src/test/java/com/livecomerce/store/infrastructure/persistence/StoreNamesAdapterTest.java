package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.domain.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreNamesAdapterTest {

    @Mock LoadStorePort loadStorePort;

    @InjectMocks StoreNamesAdapter adapter;

    @Test
    void loadNames_mapsEachStoreIdToItsName() {
        var store1 = Store.create(UUID.randomUUID(), "Mi Tienda", "mi-tienda", null, null);
        var store2 = Store.create(UUID.randomUUID(), "Otra Tienda", "otra-tienda", null, null);
        when(loadStorePort.loadByIds(Set.of(store1.getId(), store2.getId())))
                .thenReturn(List.of(store1, store2));

        var result = adapter.loadNames(Set.of(store1.getId(), store2.getId()));

        assertThat(result)
                .containsEntry(store1.getId(), "Mi Tienda")
                .containsEntry(store2.getId(), "Otra Tienda");
    }

    @Test
    void loadNames_whenStoreNotFound_omitsItFromTheMap() {
        var missingId = UUID.randomUUID();
        when(loadStorePort.loadByIds(Set.of(missingId))).thenReturn(List.of());

        var result = adapter.loadNames(Set.of(missingId));

        assertThat(result).isEmpty();
    }
}
