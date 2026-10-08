package com.livecomerce.live.application;

import com.livecomerce.live.LoadSellerNamesPort;
import com.livecomerce.live.LoadStoreNamesPort;
import com.livecomerce.live.application.port.out.ViewerCountPort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
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
    @Mock LoadStoreNamesPort  loadStoreNamesPort;
    @Mock LoadSellerNamesPort loadSellerNamesPort;
    @InjectMocks LiveFeedCardAssembler sut;

    private static final UUID SELLER_ID   = UUID.randomUUID();
    private static final UUID STORE_ID    = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private Live storeLive() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Store Live", "https://thumb", null, 60, CATEGORY_ID);
        live.start();
        return live;
    }

    private Live profileLive() {
        var live = Live.create(SELLER_ID, null, LiveContext.SELLER_PROFILE, "Profile Live", null, null, 60);
        live.start();
        return live;
    }

    @Test
    void assembleFeedCard_mapsLiveFieldsViewerCountAndStoreName() {
        var live = storeLive();
        when(viewerCountPort.get(live.getId())).thenReturn(7L);
        when(loadStoreNamesPort.loadNames(Set.of(STORE_ID))).thenReturn(Map.of(STORE_ID, "My Store"));

        var card = sut.assembleFeedCard(live);

        assertThat(card.id()).isEqualTo(live.getId());
        assertThat(card.sellerId()).isEqualTo(SELLER_ID);
        assertThat(card.storeId()).isEqualTo(STORE_ID);
        assertThat(card.title()).isEqualTo("Store Live");
        assertThat(card.sellerName()).isEqualTo("My Store");
        assertThat(card.thumbnailUrl()).isEqualTo("https://thumb");
        assertThat(card.currentViewers()).isEqualTo(7L);
        assertThat(card.startedAt()).isEqualTo(live.getStartedAt());
        assertThat(card.categoryId()).isEqualTo(CATEGORY_ID);
        verifyNoInteractions(loadSellerNamesPort);
    }

    @Test
    void assembleFeedCard_withoutStore_resolvesSellerName() {
        var live = profileLive();
        when(viewerCountPort.get(live.getId())).thenReturn(0L);
        when(loadSellerNamesPort.loadNames(Set.of(SELLER_ID))).thenReturn(Map.of(SELLER_ID, "Jane"));

        var card = sut.assembleFeedCard(live);

        assertThat(card.storeId()).isNull();
        assertThat(card.sellerName()).isEqualTo("Jane");
        verifyNoInteractions(loadStoreNamesPort);
    }

    @Test
    void assembleFeedCards_resolvesNamesInAtMostTwoBatchLookups() {
        var a = storeLive();
        var b = storeLive();
        var c = profileLive();
        when(viewerCountPort.get(any())).thenReturn(1L);
        when(loadStoreNamesPort.loadNames(Set.of(STORE_ID))).thenReturn(Map.of(STORE_ID, "My Store"));
        when(loadSellerNamesPort.loadNames(Set.of(SELLER_ID))).thenReturn(Map.of(SELLER_ID, "Jane"));
        var page = new PageImpl<>(List.of(a, b, c), PageRequest.of(0, 20), 3);

        var cards = sut.assembleFeedCards(page);

        assertThat(cards.getTotalElements()).isEqualTo(3);
        assertThat(cards.getContent()).extracting(LiveFeedCard::sellerName)
                .containsExactly("My Store", "My Store", "Jane");
        verify(loadStoreNamesPort, times(1)).loadNames(any());
        verify(loadSellerNamesPort, times(1)).loadNames(any());
    }

    @Test
    void assembleFeedCards_emptyPage_skipsNameLookups() {
        var cards = sut.assembleFeedCards(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(cards.getContent()).isEmpty();
        verifyNoInteractions(loadStoreNamesPort, loadSellerNamesPort, viewerCountPort);
    }

    @Test
    void assembleSnapshotCards_resolvesNames_withoutViewerCountLookups() {
        var a = storeLive();
        var b = profileLive();
        when(loadStoreNamesPort.loadNames(Set.of(STORE_ID))).thenReturn(Map.of(STORE_ID, "My Store"));
        when(loadSellerNamesPort.loadNames(Set.of(SELLER_ID))).thenReturn(Map.of(SELLER_ID, "Jane"));

        var cards = sut.assembleSnapshotCards(List.of(a, b));

        assertThat(cards).extracting(LiveFeedCard::id).containsExactly(a.getId(), b.getId());
        assertThat(cards).extracting(LiveFeedCard::sellerName).containsExactly("My Store", "Jane");
        assertThat(cards).extracting(LiveFeedCard::currentViewers).containsOnly(0L);
        verifyNoInteractions(viewerCountPort);
    }

    @Test
    void assembleUpcomingCards_mapsScheduledAtAndNames_withoutViewerCounts() {
        var scheduledAt = Instant.now().plusSeconds(3600);
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Soon", null, scheduledAt, 60, CATEGORY_ID);
        when(loadStoreNamesPort.loadNames(Set.of(STORE_ID))).thenReturn(Map.of(STORE_ID, "My Store"));

        var cards = sut.assembleUpcomingCards(new PageImpl<>(List.of(live), PageRequest.of(0, 20), 1));

        var card = cards.getContent().get(0);
        assertThat(card.id()).isEqualTo(live.getId());
        assertThat(card.title()).isEqualTo("Soon");
        assertThat(card.sellerName()).isEqualTo("My Store");
        assertThat(card.scheduledAt()).isEqualTo(scheduledAt);
        assertThat(card.categoryId()).isEqualTo(CATEGORY_ID);
        verify(viewerCountPort, never()).get(any());
    }
}
