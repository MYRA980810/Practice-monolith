package com.livecomerce.live.application;

import com.livecomerce.live.application.port.out.LiveFeedPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import com.livecomerce.live.domain.LiveStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveFeedSyncServiceTest {

    @Mock LoadLivePort          loadLivePort;
    @Mock LiveFeedPort          liveFeedPort;
    @Mock LiveFeedCardAssembler assembler;
    @InjectMocks LiveFeedSyncService sut;

    private static Live scheduledLive() {
        return Live.create(UUID.randomUUID(), null, LiveContext.SELLER_PROFILE, "My Live", null, null, 60, UUID.randomUUID());
    }

    private static Live onAirLive() {
        var live = scheduledLive();
        live.start();
        return live;
    }

    private static LiveFeedCard cardOf(Live live, String title, long viewers) {
        return new LiveFeedCard(live.getId(), live.getSellerId(), null, title, "Seller", null, viewers,
                Instant.parse("2026-10-06T10:00:00Z"), live.getCategoryId());
    }

    // --- sync ---

    @Test
    void sync_liveIsLive_upsertsFreshlyAssembledCard() {
        var live = onAirLive();
        var card = cardOf(live, "My Live", 3);
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(assembler.assembleFeedCard(live)).thenReturn(card);

        sut.sync(live.getId());

        verify(liveFeedPort).upsert(card);
        verify(liveFeedPort, never()).remove(any());
    }

    @Test
    void sync_liveNoLongerLive_removesIt() {
        var live = onAirLive();
        live.beginReconnecting();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        sut.sync(live.getId());

        verify(liveFeedPort).remove(live.getId());
        verify(liveFeedPort, never()).upsert(any());
    }

    @Test
    void sync_liveNotFound_removesIt() {
        var liveId = UUID.randomUUID();
        when(loadLivePort.loadById(liveId)).thenReturn(Optional.empty());

        sut.sync(liveId);

        verify(liveFeedPort).remove(liveId);
    }

    // --- rebuildIfMissing ---

    @Test
    void rebuildIfMissing_snapshotNeverBuilt_replacesAllWithEveryLiveLive() {
        var live = onAirLive();
        var card = cardOf(live, "My Live", 0);
        Page<Live> page = new PageImpl<>(List.of(live));
        when(liveFeedPort.currentVersion()).thenReturn(0L);
        when(loadLivePort.loadByStatus(eq(LiveStatus.LIVE), eq(Pageable.unpaged()))).thenReturn(page);
        when(assembler.assembleSnapshotCards(page.getContent())).thenReturn(List.of(card));

        sut.rebuildIfMissing();

        verify(liveFeedPort).replaceAll(List.of(card));
        // Snapshot cards never carry viewer counts: the per-live viewer lookups are skipped.
        verify(assembler, never()).assembleFeedCards(any());
    }

    @Test
    void rebuildIfMissing_snapshotExists_doesNothing() {
        when(liveFeedPort.currentVersion()).thenReturn(4L);

        sut.rebuildIfMissing();

        verify(liveFeedPort).currentVersion();
        verifyNoMoreInteractions(liveFeedPort);
    }

    // --- reconcile ---

    @Test
    void reconcile_syncsMissingChangedAndExtraLives_leavesMatchingOnesAlone() {
        var unchanged = onAirLive();
        var changed   = onAirLive();
        var missing   = onAirLive();
        var extraId   = UUID.randomUUID();

        Page<Live> page = new PageImpl<>(List.of(unchanged, changed, missing));
        when(liveFeedPort.currentVersion()).thenReturn(9L);
        when(loadLivePort.loadByStatus(eq(LiveStatus.LIVE), eq(Pageable.unpaged()))).thenReturn(page);
        when(assembler.assembleSnapshotCards(page.getContent())).thenReturn(List.of(
                cardOf(unchanged, "Same", 0),
                cardOf(changed, "Renamed", 0),
                cardOf(missing, "New", 0)));
        var extraCard = new LiveFeedCard(extraId, UUID.randomUUID(), null, "Gone", null, null, 0,
                Instant.parse("2026-10-06T09:00:00Z"), null);
        when(liveFeedPort.snapshot()).thenReturn(new LiveFeedSnapshot(9L, List.of(
                cardOf(unchanged, "Same", 0),
                cardOf(changed, "Old title", 0),
                extraCard), List.of()));

        var freshChanged = cardOf(changed, "Renamed", 0);
        var freshMissing = cardOf(missing, "New", 0);
        when(loadLivePort.loadById(changed.getId())).thenReturn(Optional.of(changed));
        when(loadLivePort.loadById(missing.getId())).thenReturn(Optional.of(missing));
        when(loadLivePort.loadById(extraId)).thenReturn(Optional.empty());
        when(assembler.assembleFeedCard(changed)).thenReturn(freshChanged);
        when(assembler.assembleFeedCard(missing)).thenReturn(freshMissing);

        sut.reconcile();

        verify(liveFeedPort).upsert(freshChanged);
        verify(liveFeedPort).upsert(freshMissing);
        verify(liveFeedPort).remove(extraId);
        verify(loadLivePort, never()).loadById(unchanged.getId());
        verify(liveFeedPort, never()).replaceAll(any());
    }

    @Test
    void reconcile_snapshotMissing_rebuildsInsteadOfDiffing() {
        Page<Live> page = new PageImpl<>(List.of());
        when(liveFeedPort.currentVersion()).thenReturn(0L);
        when(loadLivePort.loadByStatus(eq(LiveStatus.LIVE), eq(Pageable.unpaged()))).thenReturn(page);
        when(assembler.assembleSnapshotCards(page.getContent())).thenReturn(List.of());

        sut.reconcile();

        verify(liveFeedPort).replaceAll(List.of());
        verify(liveFeedPort, never()).snapshot();
    }

    @Test
    void reconcile_oneLiveFailsToSync_stillSyncsTheRest() {
        var first  = onAirLive();
        var second = onAirLive();
        Page<Live> page = new PageImpl<>(List.of(first, second));
        when(liveFeedPort.currentVersion()).thenReturn(1L);
        when(loadLivePort.loadByStatus(eq(LiveStatus.LIVE), eq(Pageable.unpaged()))).thenReturn(page);
        when(assembler.assembleSnapshotCards(page.getContent())).thenReturn(List.of(
                cardOf(first, "A", 0), cardOf(second, "B", 0)));
        when(liveFeedPort.snapshot()).thenReturn(new LiveFeedSnapshot(1L, List.of(), List.of()));
        when(loadLivePort.loadById(first.getId())).thenThrow(new IllegalStateException("db down"));
        when(loadLivePort.loadById(second.getId())).thenReturn(Optional.of(second));
        var secondCard = cardOf(second, "B", 0);
        when(assembler.assembleFeedCard(second)).thenReturn(secondCard);

        sut.reconcile();

        verify(liveFeedPort).upsert(secondCard);
    }
}
