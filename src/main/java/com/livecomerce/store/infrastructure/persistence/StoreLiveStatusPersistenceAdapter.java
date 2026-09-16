package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.store.application.port.out.LoadStoreLiveStatusPort;
import com.livecomerce.store.application.port.out.SaveStoreLiveStatusPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class StoreLiveStatusPersistenceAdapter implements LoadStoreLiveStatusPort, SaveStoreLiveStatusPort {

    private static final Logger log = LoggerFactory.getLogger(StoreLiveStatusPersistenceAdapter.class);

    private final StoreLiveStatusReadRepository repository;

    @Override
    @SuppressWarnings("null")
    public Map<UUID, UUID> loadActiveLiveIds(Collection<UUID> storeIds) {
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UUID> result = new HashMap<>();
        for (var entity : repository.findAllByStoreIdInAndLiveTrue(storeIds)) {
            result.put(entity.getStoreId(), entity.getLiveId());
        }
        return result;
    }

    @Override
    @Transactional
    @SuppressWarnings("null")
    public void markLive(UUID storeId, UUID liveId, Instant occurredAt) {
        var existing = repository.findById(storeId);
        if (existing.isEmpty()) {
            repository.save(StoreLiveStatusReadEntity.ofStarted(storeId, liveId, occurredAt));
            return;
        }
        var entity = existing.get();
        if (entity.applyStarted(liveId, occurredAt)) {
            repository.save(entity);
        } else {
            log.warn("Discarded stale LiveStartedEvent for store {} (live {}, occurredAt {})", storeId, liveId, occurredAt);
        }
    }

    @Override
    @Transactional
    @SuppressWarnings("null")
    public void markEnded(UUID storeId, UUID liveId, Instant occurredAt) {
        var existing = repository.findById(storeId);
        if (existing.isEmpty()) {
            repository.save(StoreLiveStatusReadEntity.ofEnded(storeId, liveId, occurredAt));
            return;
        }
        var entity = existing.get();
        if (entity.applyEnded(liveId, occurredAt)) {
            repository.save(entity);
        } else {
            log.warn("Discarded stale LiveEndedEvent for store {} (live {}, occurredAt {})", storeId, liveId, occurredAt);
        }
    }
}
