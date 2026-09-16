package com.livecomerce.store.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adapter tests mock the JPA repository, avoiding an embedded database (matches
 * StoreRankPersistenceAdapterTest precedent for this same store-card enrichment style).
 */
@ExtendWith(MockitoExtension.class)
class StoreLiveStatusPersistenceAdapterTest {

    @Mock StoreLiveStatusReadRepository repository;

    @InjectMocks StoreLiveStatusPersistenceAdapter adapter;

    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID LIVE_ID = UUID.randomUUID();

    private static final Instant T1 = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant T2 = T1.plus(5, ChronoUnit.MINUTES);

    @Test
    void loadActiveLiveIds_withEmptyInput_returnsEmptyMapWithoutQuerying() {
        var result = adapter.loadActiveLiveIds(Set.of());

        assertThat(result).isEmpty();
        verify(repository, never()).findAllByStoreIdInAndLiveTrue(any());
    }

    @Test
    void loadActiveLiveIds_returnsLiveIdPerStoreId() {
        when(repository.findAllByStoreIdInAndLiveTrue(Set.of(STORE_ID)))
                .thenReturn(List.of(StoreLiveStatusReadEntity.ofStarted(STORE_ID, LIVE_ID, T1)));

        var result = adapter.loadActiveLiveIds(Set.of(STORE_ID));

        assertThat(result).containsEntry(STORE_ID, LIVE_ID);
    }

    @Test
    void loadActiveLiveIds_withNoActiveLive_omitsStoreFromResult() {
        when(repository.findAllByStoreIdInAndLiveTrue(Set.of(STORE_ID))).thenReturn(List.of());

        var result = adapter.loadActiveLiveIds(Set.of(STORE_ID));

        assertThat(result).doesNotContainKey(STORE_ID);
    }

    @Test
    void markLive_whenNoExistingRow_createsLiveRow() {
        when(repository.findById(STORE_ID)).thenReturn(Optional.empty());

        adapter.markLive(STORE_ID, LIVE_ID, T1);

        var captor = ArgumentCaptor.forClass(StoreLiveStatusReadEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStoreId()).isEqualTo(STORE_ID);
        assertThat(captor.getValue().getLiveId()).isEqualTo(LIVE_ID);
        assertThat(captor.getValue().isLive()).isTrue();
    }

    @Test
    void markEnded_whenNoExistingRow_createsEndedRow() {
        when(repository.findById(STORE_ID)).thenReturn(Optional.empty());

        adapter.markEnded(STORE_ID, LIVE_ID, T2);

        var captor = ArgumentCaptor.forClass(StoreLiveStatusReadEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStoreId()).isEqualTo(STORE_ID);
        assertThat(captor.getValue().isLive()).isFalse();
    }

    @Test
    void markLiveThenMarkEnded_inOrder_endsUpNotLive() {
        var row = StoreLiveStatusReadEntity.ofStarted(STORE_ID, LIVE_ID, T1);
        when(repository.findById(STORE_ID)).thenReturn(Optional.empty(), Optional.of(row));

        adapter.markLive(STORE_ID, LIVE_ID, T1);
        adapter.markEnded(STORE_ID, LIVE_ID, T2);

        assertThat(row.isLive()).isFalse();
    }

    @Test
    void markEndedBeforeDelayedMarkLive_outOfOrderRedelivery_discardsStaleMarkLive() {
        // Reproduces the race: LiveEndedEvent (t2) is processed before the
        // corresponding, delayed LiveStartedEvent (t1 < t2) retry arrives.
        when(repository.findById(STORE_ID)).thenReturn(Optional.empty());

        adapter.markEnded(STORE_ID, LIVE_ID, T2);
        var createdCaptor = ArgumentCaptor.forClass(StoreLiveStatusReadEntity.class);
        verify(repository).save(createdCaptor.capture());
        var row = createdCaptor.getValue();
        assertThat(row.isLive()).isFalse();

        when(repository.findById(STORE_ID)).thenReturn(Optional.of(row));
        adapter.markLive(STORE_ID, LIVE_ID, T1);

        assertThat(row.isLive())
                .as("the delayed, stale markLive(t1 < t2) must not resurrect the store as live")
                .isFalse();
    }

    @Test
    void markEndedThenMarkLive_normalOrder_endsUpLive() {
        var row = StoreLiveStatusReadEntity.ofEnded(STORE_ID, UUID.randomUUID(), T1);
        when(repository.findById(STORE_ID)).thenReturn(Optional.of(row));

        adapter.markLive(STORE_ID, LIVE_ID, T2);

        assertThat(row.isLive()).isTrue();
        assertThat(row.getLiveId()).isEqualTo(LIVE_ID);
    }
}
