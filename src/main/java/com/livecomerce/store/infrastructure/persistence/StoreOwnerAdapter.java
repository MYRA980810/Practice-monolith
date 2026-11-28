package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.live.LoadSellerStoreIdPort;
import com.livecomerce.live.LoadStoreOwnerPort;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.domain.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Store ↔ owner resolution for the {@code live} module, in both directions. */
@Component
@RequiredArgsConstructor
class StoreOwnerAdapter implements LoadStoreOwnerPort, LoadSellerStoreIdPort {

    private final LoadStorePort loadStorePort;

    @Override
    public Optional<UUID> loadOwnerId(UUID storeId) {
        return loadStorePort.loadById(storeId).map(Store::getUserId);
    }

    @Override
    public Optional<UUID> findStoreIdBySellerId(UUID sellerId) {
        return loadStorePort.loadByUserId(sellerId).map(Store::getId);
    }

    @Override
    public Optional<UUID> findOpenStoreIdBySellerId(UUID sellerId) {
        return loadStorePort.loadByUserId(sellerId)
                .filter(Store::isOpenForSales)
                .map(Store::getId);
    }
}
