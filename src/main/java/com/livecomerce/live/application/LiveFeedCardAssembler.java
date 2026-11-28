package com.livecomerce.live.application;

import com.livecomerce.live.LoadSellerNamesPort;
import com.livecomerce.live.application.port.out.ViewerCountPort;
import com.livecomerce.live.domain.Live;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the feed cards shown for active and upcoming lives: live fields plus the
 * seller's display name (every live belongs to the seller) and, for
 * active lives, the current viewer count. Single source of card assembly for the
 * paged feed endpoints and for single-live consumers.
 */
@Component
@RequiredArgsConstructor
public class LiveFeedCardAssembler {

    private final ViewerCountPort     viewerCountPort;
    private final LoadSellerNamesPort loadSellerNamesPort;

    public Page<LiveFeedCard> assembleFeedCards(Page<Live> lives) {
        var names = resolveSellerNames(lives.getContent());
        return lives.map(live -> toFeedCard(live, names.get(live.getId())));
    }

    public LiveFeedCard assembleFeedCard(Live live) {
        var names = resolveSellerNames(List.of(live));
        return toFeedCard(live, names.get(live.getId()));
    }

    /**
     * Cards for the feed snapshot, which never carries viewer counts ({@code currentViewers} is
     * 0): only the batched name lookups run, none of the per-live viewer-count reads.
     */
    public List<LiveFeedCard> assembleSnapshotCards(List<Live> lives) {
        var names = resolveSellerNames(lives);
        return lives.stream()
                .map(live -> toFeedCard(live, names.get(live.getId()), 0L))
                .toList();
    }

    /** Fills the snapshot's {@code currentViewers} (always 0 when stored) from one batch lookup. */
    public LiveFeedSnapshot withViewerCounts(LiveFeedSnapshot snapshot) {
        if (snapshot.cards().isEmpty()) return snapshot;
        var viewers = viewerCountPort.getAll(snapshot.cards().stream().map(LiveFeedCard::id).toList());
        var cards = snapshot.cards().stream()
                .map(card -> card.withCurrentViewers(viewers.getOrDefault(card.id(), 0L)))
                .toList();
        return new LiveFeedSnapshot(snapshot.version(), cards, snapshot.counts());
    }

    public Page<LiveUpcomingCard> assembleUpcomingCards(Page<Live> lives) {
        var names = resolveSellerNames(lives.getContent());
        return lives.map(live -> toUpcomingCard(live, names.get(live.getId())));
    }

    private LiveFeedCard toFeedCard(Live live, String sellerName) {
        return toFeedCard(live, sellerName, viewerCountPort.get(live.getId()));
    }

    private LiveFeedCard toFeedCard(Live live, String sellerName, long currentViewers) {
        return new LiveFeedCard(
                live.getId(),
                live.getSellerId(),
                live.getTitle(),
                sellerName,
                live.getThumbnailUrl(),
                currentViewers,
                live.getStartedAt(),
                live.getCategoryId()
        );
    }

    private LiveUpcomingCard toUpcomingCard(Live live, String sellerName) {
        return new LiveUpcomingCard(
                live.getId(),
                live.getSellerId(),
                live.getTitle(),
                sellerName,
                live.getThumbnailUrl(),
                live.getScheduledAt(),
                live.getCategoryId()
        );
    }

    /**
     * Resolves each live's seller display name in one batch lookup regardless of page size,
     * instead of one lookup per card.
     */
    private Map<UUID, String> resolveSellerNames(List<Live> lives) {
        Set<UUID> sellerIds = lives.stream()
                .map(Live::getSellerId)
                .collect(Collectors.toSet());

        Map<UUID, String> sellerNames = sellerIds.isEmpty()
                ? Map.of()
                : loadSellerNamesPort.loadNames(sellerIds);

        Map<UUID, String> namesByLiveId = new HashMap<>();
        for (var live : lives) {
            namesByLiveId.put(live.getId(), sellerNames.get(live.getSellerId()));
        }
        return namesByLiveId;
    }
}
