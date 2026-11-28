package com.livecomerce.live.application;

import com.livecomerce.live.LoadSellerNamesPort;
import com.livecomerce.live.application.port.out.LoadLivePort.CategoryLiveCount;
import com.livecomerce.live.application.port.out.ViewerCountPort;
import com.livecomerce.live.domain.Live;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveFeedCardAssemblerTest {

    @Mock ViewerCountPort     viewerCountPort;
    @Mock LoadSellerNamesPort loadSellerNamesPort;
    @InjectMocks LiveFeedCardAssembler sut;

    private static final UUID SELLER_ID   = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private static final UUID OTHER_SELLER_ID = UUID.randomUUID();

    private Live live(UUID sellerId, String title) {
        var live = Live.create(sellerId, title, "https://thumb", null, 60, CATEGORY_ID);
        live.start();
        return live;
    }

    @Test
    void assembleFeedCard_mapsLiveFieldsViewerCountAndSellerName() {
        var live = live(SELLER_ID, "Seller Live");
        when(viewerCountPort.get(live.getId())).thenReturn(7L);
        when(loadSellerNamesPort.loadNames(Set.of(SELLER_ID))).thenReturn(Map.of(SELLER_ID, "Jane"));

        var card = sut.assembleFeedCard(live);

        assertThat(card.id()).isEqualTo(live.getId());
        assertThat(card.sellerId()).isEqualTo(SELLER_ID);
        assertThat(card.title()).isEqualTo("Seller Live");
        assertThat(card.sellerName()).isEqualTo("Jane");
        assertThat(card.thumbnailUrl()).isEqualTo("https://thumb");
        assertThat(card.currentViewers()).isEqualTo(7L);
        assertThat(card.startedAt()).isEqualTo(live.getStartedAt());
        assertThat(card.categoryId()).isEqualTo(CATEGORY_ID);
    }

    @Test
    void assembleFeedCards_resolvesSellerNamesInOneBatchLookup() {
        var a = live(SELLER_ID, "A");
        var b = live(SELLER_ID, "B");
        var c = live(OTHER_SELLER_ID, "C");
        when(viewerCountPort.get(any())).thenReturn(1L);
        when(loadSellerNamesPort.loadNames(Set.of(SELLER_ID, OTHER_SELLER_ID)))
                .thenReturn(Map.of(SELLER_ID, "Jane", OTHER_SELLER_ID, "John"));
        var page = new PageImpl<>(List.of(a, b, c), PageRequest.of(0, 20), 3);

        var cards = sut.assembleFeedCards(page);

        assertThat(cards.getTotalElements()).isEqualTo(3);
        assertThat(cards.getContent()).extracting(LiveFeedCard::sellerName)
                .containsExactly("Jane", "Jane", "John");
        verify(loadSellerNamesPort, times(1)).loadNames(any());
    }

    @Test
    void assembleFeedCards_emptyPage_skipsNameLookups() {
        var cards = sut.assembleFeedCards(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(cards.getContent()).isEmpty();
        verifyNoInteractions(loadSellerNamesPort, viewerCountPort);
    }

    @Test
    void assembleSnapshotCards_resolvesNames_withoutViewerCountLookups() {
        var a = live(SELLER_ID, "A");
        var b = live(OTHER_SELLER_ID, "B");
        when(loadSellerNamesPort.loadNames(Set.of(SELLER_ID, OTHER_SELLER_ID)))
                .thenReturn(Map.of(SELLER_ID, "Jane", OTHER_SELLER_ID, "John"));

        var cards = sut.assembleSnapshotCards(List.of(a, b));

        assertThat(cards).extracting(LiveFeedCard::id).containsExactly(a.getId(), b.getId());
        assertThat(cards).extracting(LiveFeedCard::sellerName).containsExactly("Jane", "John");
        assertThat(cards).extracting(LiveFeedCard::currentViewers).containsOnly(0L);
        verifyNoInteractions(viewerCountPort);
    }

    @Test
    void withViewerCounts_fillsEveryCardFromOneBatchLookup() {
        var a = card(UUID.randomUUID());
        var b = card(UUID.randomUUID());
        var counts = List.of(new CategoryLiveCount(CATEGORY_ID, 2));
        when(viewerCountPort.getAll(List.of(a.id(), b.id()))).thenReturn(Map.of(a.id(), 9L));

        var snapshot = sut.withViewerCounts(new LiveFeedSnapshot(12L, List.of(a, b), counts));

        assertThat(snapshot.version()).isEqualTo(12L);
        assertThat(snapshot.counts()).isEqualTo(counts);
        assertThat(snapshot.cards()).extracting(LiveFeedCard::id).containsExactly(a.id(), b.id());
        assertThat(snapshot.cards()).extracting(LiveFeedCard::currentViewers).containsExactly(9L, 0L);
        verify(viewerCountPort, never()).get(any());
    }

    @Test
    void withViewerCounts_emptySnapshot_skipsLookup() {
        var empty = new LiveFeedSnapshot(3L, List.of(), List.of());

        assertThat(sut.withViewerCounts(empty)).isEqualTo(empty);
        verifyNoInteractions(viewerCountPort);
    }

    private static LiveFeedCard card(UUID id) {
        return new LiveFeedCard(id, SELLER_ID, "Live", "Jane", null, 0L, Instant.now(), CATEGORY_ID);
    }

    @Test
    void assembleUpcomingCards_mapsScheduledAtAndNames_withoutViewerCounts() {
        var scheduledAt = Instant.now().plusSeconds(3600);
        var live = Live.create(SELLER_ID, "Soon", null, scheduledAt, 60, CATEGORY_ID);
        when(loadSellerNamesPort.loadNames(Set.of(SELLER_ID))).thenReturn(Map.of(SELLER_ID, "Jane"));

        var cards = sut.assembleUpcomingCards(new PageImpl<>(List.of(live), PageRequest.of(0, 20), 1));

        var card = cards.getContent().get(0);
        assertThat(card.id()).isEqualTo(live.getId());
        assertThat(card.title()).isEqualTo("Soon");
        assertThat(card.sellerName()).isEqualTo("Jane");
        assertThat(card.scheduledAt()).isEqualTo(scheduledAt);
        assertThat(card.categoryId()).isEqualTo(CATEGORY_ID);
        verify(viewerCountPort, never()).get(any());
    }
}
