package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.store.application.port.out.LoadStoreRankPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class StoreRankPersistenceAdapter implements LoadStoreRankPort {

    private final StoreRankingSnapshotReadRepository repository;

    @Override
    public Map<UUID, Integer> loadRanks(Collection<UUID> storeIds) {
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        var latest = repository.findLatestComputedAt();
        if (latest == null) {
            return Map.of();
        }
        Map<UUID, Integer> ranks = new HashMap<>();
        for (var snapshot : repository.findByStoreIdInAndComputedAt(storeIds, latest)) {
            ranks.put(snapshot.getStoreId(), snapshot.getRank());
        }
        return ranks;
    }
}
