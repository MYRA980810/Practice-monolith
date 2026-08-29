package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.SaveStorePort;
import com.livecomerce.store.domain.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class StorePersistenceAdapter implements LoadStorePort, SaveStorePort {

    private final StoreJpaRepository repository;

    @Override
    public Optional<Store> loadByUserId(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public Optional<Store> loadBySlug(String slug) {
        return repository.findBySlug(slug);
    }

    @Override
    @SuppressWarnings("null")
    public Optional<Store> loadById(UUID storeId) {
        return repository.findById(storeId);
    }

    @Override
    @SuppressWarnings("null")
    public List<Store> loadByIds(Collection<UUID> storeIds) {
        return repository.findAllById(storeIds);
    }

    @Override
    public Page<Store> loadAllActive(Pageable pageable) {
        return repository.findAllByActiveTrueAndTemporarilyClosedFalse(pageable);
    }

    @Override
    @SuppressWarnings("null")
    public Store save(Store store) {
        return repository.save(store);
    }
}
