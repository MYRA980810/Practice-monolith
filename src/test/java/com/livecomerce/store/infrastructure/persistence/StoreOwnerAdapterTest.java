package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.domain.Store;
import com.livecomerce.store.domain.SuspensionReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreOwnerAdapterTest {

    @Mock LoadStorePort loadStorePort;

    @InjectMocks StoreOwnerAdapter adapter;

    @Test
    void loadOwnerId_returnsTheStoresUserId() {
        var ownerId = UUID.randomUUID();
        var store = Store.create(ownerId, "My Store", "my-store", null, null);
        when(loadStorePort.loadById(store.getId())).thenReturn(Optional.of(store));

        assertThat(adapter.loadOwnerId(store.getId())).contains(ownerId);
    }

    @Test
    void loadOwnerId_unknownStore_returnsEmpty() {
        var storeId = UUID.randomUUID();
        when(loadStorePort.loadById(storeId)).thenReturn(Optional.empty());

        assertThat(adapter.loadOwnerId(storeId)).isEmpty();
    }

    @Test
    void findStoreIdBySellerId_returnsTheSellersStoreId() {
        var sellerId = UUID.randomUUID();
        var store = Store.create(sellerId, "My Store", "my-store", null, null);
        when(loadStorePort.loadByUserId(sellerId)).thenReturn(Optional.of(store));

        assertThat(adapter.findStoreIdBySellerId(sellerId)).contains(store.getId());
    }

    @Test
    void findStoreIdBySellerId_sellerWithoutStore_returnsEmpty() {
        var sellerId = UUID.randomUUID();
        when(loadStorePort.loadByUserId(sellerId)).thenReturn(Optional.empty());

        assertThat(adapter.findStoreIdBySellerId(sellerId)).isEmpty();
    }

    @Test
    void findOpenStoreIdBySellerId_openStore_returnsItsId() {
        var sellerId = UUID.randomUUID();
        var store = Store.create(sellerId, "My Store", "my-store", null, null);
        when(loadStorePort.loadByUserId(sellerId)).thenReturn(Optional.of(store));

        assertThat(adapter.findOpenStoreIdBySellerId(sellerId)).contains(store.getId());
    }

    @Test
    void findOpenStoreIdBySellerId_sellerWithoutStore_returnsEmpty() {
        var sellerId = UUID.randomUUID();
        when(loadStorePort.loadByUserId(sellerId)).thenReturn(Optional.empty());

        assertThat(adapter.findOpenStoreIdBySellerId(sellerId)).isEmpty();
    }

    @Test
    void findOpenStoreIdBySellerId_deactivatedStore_returnsEmpty() {
        var sellerId = UUID.randomUUID();
        var store = Store.create(sellerId, "My Store", "my-store", null, null);
        store.deactivate();
        when(loadStorePort.loadByUserId(sellerId)).thenReturn(Optional.of(store));

        assertThat(adapter.findOpenStoreIdBySellerId(sellerId)).isEmpty();
    }

    @Test
    void findOpenStoreIdBySellerId_suspendedStore_returnsEmpty() {
        var sellerId = UUID.randomUUID();
        var store = Store.create(sellerId, "My Store", "my-store", null, null);
        store.suspend(SuspensionReason.BILLING);
        when(loadStorePort.loadByUserId(sellerId)).thenReturn(Optional.of(store));

        assertThat(adapter.findOpenStoreIdBySellerId(sellerId)).isEmpty();
    }

    @Test
    void findOpenStoreIdBySellerId_temporarilyClosedStore_returnsEmpty() {
        var sellerId = UUID.randomUUID();
        var store = Store.create(sellerId, "My Store", "my-store", null, null);
        store.closeTemporarily();
        when(loadStorePort.loadByUserId(sellerId)).thenReturn(Optional.of(store));

        assertThat(adapter.findOpenStoreIdBySellerId(sellerId)).isEmpty();
    }
}
