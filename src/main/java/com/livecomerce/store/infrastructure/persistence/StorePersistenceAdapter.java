package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.SaveStorePort;
import com.livecomerce.store.domain.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    public Store save(Store store) {
        return repository.save(store);
    }
}
