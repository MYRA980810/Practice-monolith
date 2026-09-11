package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.domain.StoreRankingSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.UUID;

interface StoreRankingJpaRepository extends JpaRepository<StoreRankingSnapshot, UUID> {

    @Query("SELECT MAX(s.computedAt) FROM StoreRankingSnapshot s")
    OffsetDateTime findLatestComputedAt();

    Page<StoreRankingSnapshot> findByComputedAtOrderByRankAsc(OffsetDateTime computedAt, Pageable pageable);
}
