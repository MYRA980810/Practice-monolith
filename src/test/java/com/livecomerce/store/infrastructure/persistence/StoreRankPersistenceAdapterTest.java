package com.livecomerce.store.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adapter tests use Mockito to mock the JPA repository, avoiding the need for an
 * embedded database or Flyway migrations in tests (matches StoreRankingPersistenceAdapterTest
 * precedent in the analytics module, which owns the same store_ranking_snapshots table).
 */
@ExtendWith(MockitoExtension.class)
class StoreRankPersistenceAdapterTest {

    @Mock StoreRankingSnapshotReadRepository repository;

    @InjectMocks StoreRankPersistenceAdapter adapter;

    private static final UUID STORE_ID_1 = UUID.randomUUID();
    private static final UUID STORE_ID_2 = UUID.randomUUID();

    @Test
    void loadRanks_withEmptyInput_returnsEmptyMapWithoutQuerying() {
        var ranks = adapter.loadRanks(Set.of());

        assertThat(ranks).isEmpty();
        verify(repository, never()).findLatestComputedAt();
    }

    @Test
    void loadRanks_whenNoSnapshotsExist_returnsEmptyMap() {
        when(repository.findLatestComputedAt()).thenReturn(null);

        var ranks = adapter.loadRanks(Set.of(STORE_ID_1));

        assertThat(ranks).isEmpty();
    }

    @Test
    void loadRanks_returnsRankPerStoreIdFromLatestSnapshot() {
        var latest = OffsetDateTime.now();
        when(repository.findLatestComputedAt()).thenReturn(latest);
        when(repository.findByStoreIdInAndComputedAt(eq(Set.of(STORE_ID_1, STORE_ID_2)), eq(latest)))
                .thenReturn(List.of(
                        new StoreRankingSnapshotReadEntity(STORE_ID_1, 1, latest),
                        new StoreRankingSnapshotReadEntity(STORE_ID_2, 2, latest)
                ));

        var ranks = adapter.loadRanks(Set.of(STORE_ID_1, STORE_ID_2));

        assertThat(ranks).containsEntry(STORE_ID_1, 1).containsEntry(STORE_ID_2, 2);
    }

    @Test
    void loadRanks_withUnrankedStore_omitsItFromResultMap() {
        var latest = OffsetDateTime.now();
        when(repository.findLatestComputedAt()).thenReturn(latest);
        when(repository.findByStoreIdInAndComputedAt(any(), eq(latest)))
                .thenReturn(List.of(new StoreRankingSnapshotReadEntity(STORE_ID_1, 1, latest)));

        var ranks = adapter.loadRanks(Set.of(STORE_ID_1, STORE_ID_2));

        assertThat(ranks).containsOnlyKeys(STORE_ID_1);
    }
}
