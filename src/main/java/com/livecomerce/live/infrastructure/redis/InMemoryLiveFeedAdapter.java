package com.livecomerce.live.infrastructure.redis;

import com.livecomerce.live.application.LiveFeedCard;
import com.livecomerce.live.application.LiveFeedEvent;
import com.livecomerce.live.application.LiveFeedSnapshot;
import com.livecomerce.live.application.port.out.LiveFeedPort;
import com.livecomerce.live.application.port.out.LoadLivePort.CategoryLiveCount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

/**
 * Single-instance twin of {@link RedisLiveFeedAdapter} for the {@code local} profile.
 * Every write runs under the instance lock and delivers its event to the local sinks
 * before releasing it, so events reach sinks in {@code seq} order. Version seeding mirrors
 * the Redis adapter: a rebuild, or the first write after a restart, never goes below the
 * current epoch millis. The reconcile lease is always granted — there is a single instance.
 */
@Component
@Profile("local")
class InMemoryLiveFeedAdapter implements LiveFeedPort {

    /** Same order as the REST counts: count desc, then category id as text (UUID#compareTo is signed). */
    private static final Comparator<CategoryLiveCount> COUNT_ORDER =
            Comparator.comparingLong(CategoryLiveCount::count).reversed()
                    .thenComparing(count -> count.categoryId().toString());

    private final LiveFeedEventDispatcher dispatcher;
    private final LongSupplier            nowMillis;
    private final Map<UUID, LiveFeedCard> cards = new HashMap<>();
    private long version;

    @Autowired
    InMemoryLiveFeedAdapter(LiveFeedEventDispatcher dispatcher) {
        this(dispatcher, System::currentTimeMillis);
    }

    InMemoryLiveFeedAdapter(LiveFeedEventDispatcher dispatcher, LongSupplier nowMillis) {
        this.dispatcher = dispatcher;
        this.nowMillis  = nowMillis;
    }

    @Override
    public synchronized LiveFeedEvent upsert(LiveFeedCard card) {
        var stored = card.withCurrentViewers(0);
        cards.put(stored.id(), stored);
        return publish(new LiveFeedEvent.Added(nextVersion(version == 0L), stored, counts()));
    }

    @Override
    public synchronized Optional<LiveFeedEvent> remove(UUID liveId) {
        if (cards.remove(liveId) == null) {
            return Optional.empty();
        }
        return Optional.of(publish(new LiveFeedEvent.Removed(nextVersion(version == 0L), liveId, counts())));
    }

    @Override
    public synchronized LiveFeedSnapshot snapshot() {
        var ordered = cards.values().stream()
                .sorted(Comparator.comparing(LiveFeedCard::startedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return new LiveFeedSnapshot(version, ordered, counts());
    }

    @Override
    public synchronized long currentVersion() {
        return version;
    }

    @Override
    public synchronized LiveFeedEvent replaceAll(List<LiveFeedCard> newCards) {
        cards.clear();
        newCards.forEach(card -> cards.put(card.id(), card.withCurrentViewers(0)));
        return publish(new LiveFeedEvent.Resynced(nextVersion(true)));
    }

    @Override
    public boolean tryAcquireReconcileLock(Duration ttl) {
        return true;
    }

    /** Increments the version, raising it to the current epoch millis when {@code seed} and it's behind. */
    private long nextVersion(boolean seed) {
        version = seed ? Math.max(version + 1, nowMillis.getAsLong()) : version + 1;
        return version;
    }

    private List<CategoryLiveCount> counts() {
        return cards.values().stream()
                .map(LiveFeedCard::categoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new CategoryLiveCount(entry.getKey(), entry.getValue()))
                .sorted(COUNT_ORDER)
                .toList();
    }

    private LiveFeedEvent publish(LiveFeedEvent event) {
        dispatcher.deliver(event);
        return event;
    }
}
