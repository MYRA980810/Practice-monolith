package com.livecomerce.live.infrastructure.redis;

import com.livecomerce.live.application.LiveFeedCard;
import com.livecomerce.live.application.LiveFeedEvent;
import com.livecomerce.live.application.port.out.LoadLivePort.CategoryLiveCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InMemoryLiveFeedAdapterTest {

    @Mock LiveFeedEventDispatcher dispatcher;

    InMemoryLiveFeedAdapter adapter;

    /** Wall clock seen by the adapter; small by default so plain writes keep counting from 1. */
    long nowMillis = 0L;

    private static final UUID CATEGORY_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID CATEGORY_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @BeforeEach
    void setUp() {
        adapter = new InMemoryLiveFeedAdapter(dispatcher, () -> nowMillis);
    }

    private static LiveFeedCard card(UUID liveId, UUID categoryId) {
        return new LiveFeedCard(liveId, UUID.randomUUID(), null, "Live " + liveId, "Seller", null,
                42L, Instant.parse("2026-10-06T10:00:00Z"), categoryId);
    }

    @Test
    void upsert_newLive_bumpsVersion_storesCardWithoutViewers_andCountsCategory() {
        var liveId = UUID.randomUUID();

        var event = adapter.upsert(card(liveId, CATEGORY_A));

        assertThat(event).isInstanceOf(LiveFeedEvent.Added.class);
        var added = (LiveFeedEvent.Added) event;
        assertThat(added.seq()).isEqualTo(1L);
        assertThat(added.card().id()).isEqualTo(liveId);
        assertThat(added.card().currentViewers()).isZero();
        assertThat(added.counts()).containsExactly(new CategoryLiveCount(CATEGORY_A, 1));
        assertThat(adapter.currentVersion()).isEqualTo(1L);
        verify(dispatcher).deliver(added);
    }

    @Test
    void upsert_existingLiveWithNewCategory_movesCountBetweenCategories() {
        var liveId = UUID.randomUUID();
        adapter.upsert(card(liveId, CATEGORY_A));

        var event = (LiveFeedEvent.Added) adapter.upsert(card(liveId, CATEGORY_B));

        assertThat(event.seq()).isEqualTo(2L);
        assertThat(event.counts()).containsExactly(new CategoryLiveCount(CATEGORY_B, 1));
        assertThat(adapter.snapshot().cards()).hasSize(1);
    }

    @Test
    void upsert_uncategorizedLive_isNotCounted() {
        var event = (LiveFeedEvent.Added) adapter.upsert(card(UUID.randomUUID(), null));

        assertThat(event.counts()).isEmpty();
    }

    @Test
    void counts_areOrderedByCountDescThenCategoryIdAsc() {
        adapter.upsert(card(UUID.randomUUID(), CATEGORY_B));
        adapter.upsert(card(UUID.randomUUID(), CATEGORY_A));
        var event = (LiveFeedEvent.Added) adapter.upsert(card(UUID.randomUUID(), CATEGORY_B));

        assertThat(event.counts()).containsExactly(
                new CategoryLiveCount(CATEGORY_B, 2),
                new CategoryLiveCount(CATEGORY_A, 1));
    }

    @Test
    void remove_presentLive_bumpsVersion_dropsCard_andZeroCountDisappears() {
        var liveId = UUID.randomUUID();
        adapter.upsert(card(liveId, CATEGORY_A));

        var event = adapter.remove(liveId);

        assertThat(event).containsInstanceOf(LiveFeedEvent.Removed.class);
        var removed = (LiveFeedEvent.Removed) event.orElseThrow();
        assertThat(removed.seq()).isEqualTo(2L);
        assertThat(removed.liveId()).isEqualTo(liveId);
        assertThat(removed.counts()).isEmpty();
        assertThat(adapter.snapshot().cards()).isEmpty();
        verify(dispatcher).deliver(removed);
    }

    @Test
    void remove_absentLive_isNoOp_noVersionBump_noDelivery() {
        var event = adapter.remove(UUID.randomUUID());

        assertThat(event).isEmpty();
        assertThat(adapter.currentVersion()).isZero();
        verify(dispatcher, never()).deliver(any());
    }

    @Test
    void snapshot_returnsVersionCardsNewestFirstAndCounts() {
        var older = new LiveFeedCard(UUID.randomUUID(), UUID.randomUUID(), null, "Older", null, null, 0,
                Instant.parse("2026-10-06T09:00:00Z"), CATEGORY_A);
        var newer = new LiveFeedCard(UUID.randomUUID(), UUID.randomUUID(), null, "Newer", null, null, 0,
                Instant.parse("2026-10-06T11:00:00Z"), CATEGORY_A);
        adapter.upsert(older);
        adapter.upsert(newer);

        var snapshot = adapter.snapshot();

        assertThat(snapshot.version()).isEqualTo(2L);
        assertThat(snapshot.cards()).extracting(LiveFeedCard::id).containsExactly(newer.id(), older.id());
        assertThat(snapshot.counts()).containsExactly(new CategoryLiveCount(CATEGORY_A, 2));
    }

    @Test
    void replaceAll_rebuildsCardsAndCounts_bumpsVersion_andDeliversResync() {
        var stale = UUID.randomUUID();
        adapter.upsert(card(stale, CATEGORY_A));
        var fresh = card(UUID.randomUUID(), CATEGORY_B);

        var event = adapter.replaceAll(List.of(fresh));

        assertThat(event).isEqualTo(new LiveFeedEvent.Resynced(2L));
        var snapshot = adapter.snapshot();
        assertThat(snapshot.version()).isEqualTo(2L);
        assertThat(snapshot.cards()).extracting(LiveFeedCard::id).containsExactly(fresh.id());
        assertThat(snapshot.counts()).containsExactly(new CategoryLiveCount(CATEGORY_B, 1));

        var captor = ArgumentCaptor.forClass(LiveFeedEvent.class);
        verify(dispatcher, times(2)).deliver(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new LiveFeedEvent.Resynced(2L));
    }

    @Test
    void currentVersion_beforeAnyWrite_isZero() {
        assertThat(adapter.currentVersion()).isZero();
    }

    @Test
    void replaceAll_afterRestart_seedsVersionFromClock_soItNeverRegresses() {
        nowMillis = 1_760_000_000_000L;

        var event = adapter.replaceAll(List.of(card(UUID.randomUUID(), CATEGORY_A)));

        assertThat(event).isEqualTo(new LiveFeedEvent.Resynced(nowMillis));
        assertThat(adapter.currentVersion()).isEqualTo(nowMillis);
        var next = (LiveFeedEvent.Added) adapter.upsert(card(UUID.randomUUID(), CATEGORY_A));
        assertThat(next.seq()).isEqualTo(nowMillis + 1);
    }

    @Test
    void replaceAll_versionAheadOfClock_keepsIncrementing() {
        nowMillis = 1_760_000_000_000L;
        adapter.replaceAll(List.of());
        nowMillis -= 5_000L; // clock skew between replicas / NTP step back

        var event = adapter.replaceAll(List.of());

        assertThat(event).isEqualTo(new LiveFeedEvent.Resynced(1_760_000_000_001L));
    }

    @Test
    void firstWriteOnEmptyFeed_seedsVersionFromClock() {
        nowMillis = 1_760_000_000_000L;

        var event = (LiveFeedEvent.Added) adapter.upsert(card(UUID.randomUUID(), CATEGORY_A));

        assertThat(event.seq()).isEqualTo(nowMillis);
    }

    @Test
    void reconcileLock_singleInstance_isAlwaysAcquired() {
        assertThat(adapter.tryAcquireReconcileLock(Duration.ofSeconds(54))).isTrue();
        assertThat(adapter.tryAcquireReconcileLock(Duration.ofSeconds(54))).isTrue();
    }
}
