package com.livecomerce.store.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only view over the shared {@code store_ranking_snapshots} table (owned by
 * {@code analytics}), scoped to the columns needed to resolve a store's current
 * ranking position — never importing {@code analytics.domain.StoreRankingSnapshot}
 * (see {@link com.livecomerce.store.application.port.out.LoadStoreRankPort} for the
 * module-cycle rationale).
 */
@Entity
@Immutable
@Table(name = "store_ranking_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class StoreRankingSnapshotReadEntity {

    @Id
    private UUID id;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "rank")
    private int rank;

    @Column(name = "computed_at")
    private OffsetDateTime computedAt;

    // Package-private, test-only construction: Hibernate populates real instances via
    // field access through the protected no-args constructor above; this constructor
    // exists only so StoreRankPersistenceAdapterTest (same package) can build fixtures
    // without a live database.
    StoreRankingSnapshotReadEntity(UUID storeId, int rank, OffsetDateTime computedAt) {
        this.id = UUID.randomUUID();
        this.storeId = storeId;
        this.rank = rank;
        this.computedAt = computedAt;
    }
}
