package com.livecomerce.order.infrastructure.persistence;

import com.livecomerce.order.application.port.out.LoadStoreIdPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class StoreIdPersistenceAdapter implements LoadStoreIdPort {

    private final StoreReadRepository repository;

    @Override
    public Optional<UUID> findStoreIdByUserId(UUID userId) {
        return repository.findByUserId(userId).map(StoreReadEntity::getId);
    }
}
