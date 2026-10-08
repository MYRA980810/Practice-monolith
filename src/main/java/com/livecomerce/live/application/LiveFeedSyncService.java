package com.livecomerce.live.application;

import com.livecomerce.live.application.port.out.LiveFeedPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.domain.LiveStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Keeps the active-lives feed snapshot ({@link LiveFeedPort}) aligned with the database,
 * which stays the source of truth.
 *
 * <p>{@link #sync} is order-robust under async, at-least-once event delivery: the domain
 * event is only a trigger, and the live's <em>current</em> status decides the outcome
 * (LIVE → upsert its freshly assembled card, anything else or gone → remove). A stale or
 * redelivered event therefore can't resurrect an ended live. The remaining window — an
 * event reloading LIVE just before a concurrent end commits and its removal lands first —
 * is closed by {@link #reconcile} on the next cycle.
 */
@Service
@RequiredArgsConstructor
public class LiveFeedSyncService {

    private static final Logger log = LoggerFactory.getLogger(LiveFeedSyncService.class);

    private final LoadLivePort          loadLivePort;
    private final LiveFeedPort          liveFeedPort;
    private final LiveFeedCardAssembler assembler;

    public void sync(UUID liveId) {
        var live = loadLivePort.loadById(liveId);
        if (live.isPresent() && live.get().getStatus() == LiveStatus.LIVE) {
            liveFeedPort.upsert(assembler.assembleFeedCard(live.get()));
        } else {
            liveFeedPort.remove(liveId);
        }
    }

    /** Builds the snapshot from the database when it has never been built (fresh or flushed Redis). */
    public void rebuildIfMissing() {
        if (liveFeedPort.currentVersion() == 0L) {
            rebuild();
        }
    }

    /**
     * Fixes drift between the snapshot and the database: every live that is missing, extra or
     * whose card differs (viewer counts ignored) is re-synced through {@link #sync}, which
     * re-reads it so the decision uses its latest state. The full read is one batched query
     * plus the assembler's batched name lookups; per-live reloads happen only for drifted ones.
     */
    public void reconcile() {
        if (liveFeedPort.currentVersion() == 0L) {
            rebuild();
            return;
        }

        Map<UUID, LiveFeedCard> expected = assembleAllLiveCards().stream()
                .collect(Collectors.toMap(LiveFeedCard::id, Function.identity()));
        Map<UUID, LiveFeedCard> actual = liveFeedPort.snapshot().cards().stream()
                .collect(Collectors.toMap(LiveFeedCard::id, Function.identity()));

        var drifted = new LinkedHashSet<UUID>();
        expected.forEach((id, card) -> {
            if (!card.equals(actual.get(id))) drifted.add(id);
        });
        var extra = new HashSet<>(actual.keySet());
        extra.removeAll(expected.keySet());
        drifted.addAll(extra);

        if (drifted.isEmpty()) {
            return;
        }
        log.info("Live feed reconciliation: re-syncing {} drifted live(s)", drifted.size());
        for (var liveId : drifted) {
            try {
                sync(liveId);
            } catch (Exception e) {
                log.warn("Live feed reconciliation failed for live {}: {}", liveId, e.getMessage());
            }
        }
    }

    private void rebuild() {
        var cards = assembleAllLiveCards();
        liveFeedPort.replaceAll(cards);
        log.info("Live feed snapshot rebuilt from database with {} live(s)", cards.size());
    }

    /** Snapshot cards never carry viewer counts, so they're assembled without them to compare like with like. */
    private List<LiveFeedCard> assembleAllLiveCards() {
        var lives = loadLivePort.loadByStatus(LiveStatus.LIVE, Pageable.unpaged());
        return assembler.assembleSnapshotCards(lives.getContent());
    }
}
