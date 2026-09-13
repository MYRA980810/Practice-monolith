package com.livecomerce.store.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface StoreRankingSnapshotReadRepository extends JpaRepository<StoreRankingSnapshotReadEntity, UUID> {

    @Query("SELECT MAX(s.computedAt) FROM StoreRankingSnapshotReadEntity s")
    OffsetDateTime findLatestComputedAt();

    List<StoreRankingSnapshotReadEntity> findByStoreIdInAndComputedAt(Collection<UUID> storeIds, OffsetDateTime computedAt);
}
