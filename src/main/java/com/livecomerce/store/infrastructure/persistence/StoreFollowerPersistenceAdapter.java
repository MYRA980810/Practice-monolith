package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.store.application.port.out.StoreFollowerPort;
import com.livecomerce.store.domain.StoreFollower;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class StoreFollowerPersistenceAdapter implements StoreFollowerPort {

    private final StoreFollowerJpaRepository repository;

    @Override
    @SuppressWarnings("null")
    public void saveFollower(StoreFollower follower) {
        repository.save(follower);
    }

    @Override
    @Transactional
    public void deleteFollower(UUID storeId, UUID userId) {
        repository.deleteByStoreIdAndUserId(storeId, userId);
    }

    @Override
    public boolean existsFollower(UUID storeId, UUID userId) {
        return repository.existsByStoreIdAndUserId(storeId, userId);
    }

    @Override
    public long countFollowers(UUID storeId) {
        return repository.countByStoreId(storeId);
    }

    @Override
    public List<UUID> findFollowedStoreIds(UUID userId) {
        return repository.findStoreIdsByUserId(userId);
    }

    @Override
    public Map<UUID, Long> countFollowersByStoreIds(Collection<UUID> storeIds) {
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : repository.countByStoreIds(storeIds)) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }
}
