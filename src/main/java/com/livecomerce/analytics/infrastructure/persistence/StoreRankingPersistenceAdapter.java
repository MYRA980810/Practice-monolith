package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.application.port.out.LoadStoreRankingPort;
import com.livecomerce.analytics.application.port.out.SaveStoreRankingPort;
import com.livecomerce.analytics.domain.StoreRankingSnapshot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
class StoreRankingPersistenceAdapter implements SaveStoreRankingPort, LoadStoreRankingPort {

    private static final Logger log = LoggerFactory.getLogger(StoreRankingPersistenceAdapter.class);

    private final StoreRankingJpaRepository repository;

    @Override
    @Transactional
    public void replaceAll(List<StoreRankingSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            log.warn("Skipping store ranking replace: snapshots list is empty, leaving existing ranking data untouched");
            return;
        }
        repository.deleteAllInBatch();
        repository.saveAll(snapshots);
    }

    @Override
    public Page<StoreRankingSnapshot> loadLatest(Pageable pageable) {
        var latest = repository.findLatestComputedAt();
        if (latest == null) {
            return Page.empty(pageable);
        }
        return repository.findByComputedAtOrderByRankAsc(latest, pageable);
    }
}
