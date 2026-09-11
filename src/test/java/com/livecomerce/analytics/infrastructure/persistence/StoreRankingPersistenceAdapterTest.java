package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.domain.StoreRankingSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Adapter tests use Mockito to mock the JPA repository, avoiding the need for an
 * embedded database or Flyway migrations in tests (matches LiveSummaryPersistenceAdapterTest
 * precedent).
 */
@ExtendWith(MockitoExtension.class)
class StoreRankingPersistenceAdapterTest {

    @Mock StoreRankingJpaRepository repository;

    @InjectMocks StoreRankingPersistenceAdapter adapter;

    @Test
    void replaceAll_withEmptyList_doesNotDeleteOrSaveExistingData() {
        // R4-002: an empty snapshots list (legitimate min-reviews exclusion, or a silent
        // partial upstream failure) must never wipe existing ranking data.
        adapter.replaceAll(List.of());

        verify(repository, never()).deleteAllInBatch();
        verify(repository, never()).saveAll(org.mockito.ArgumentMatchers.<List<StoreRankingSnapshot>>any());
    }

    @Test
    void replaceAll_withNonEmptyList_deletesThenSaves() {
        var snapshot = StoreRankingSnapshot.create(UUID.randomUUID(), 1, new BigDecimal("1.000000"),
                new BigDecimal("5.00"), 5, 10L, new BigDecimal("100.00"), OffsetDateTime.now());

        adapter.replaceAll(List.of(snapshot));

        verify(repository).deleteAllInBatch();
        verify(repository).saveAll(List.of(snapshot));
    }
}
