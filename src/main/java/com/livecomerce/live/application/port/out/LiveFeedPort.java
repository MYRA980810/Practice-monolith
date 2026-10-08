package com.livecomerce.live.application.port.out;

import com.livecomerce.live.application.LiveFeedCard;
import com.livecomerce.live.application.LiveFeedEvent;
import com.livecomerce.live.application.LiveFeedSnapshot;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Versioned snapshot of the active-lives feed plus fan-out of its changes. Every write
 * atomically applies the change, bumps the version and publishes the resulting {@link
 * LiveFeedEvent} to every instance's {@link com.livecomerce.live.application.port.in.LiveFeedEventSink}s,
 * so the snapshot and the event stream never diverge. The database stays the source of
 * truth; this is a cache rebuilt by {@link #replaceAll} and drift reconciliation.
 */
public interface LiveFeedPort {

    /** Adds the card or replaces the stored one (category moves are reflected in the counts). */
    LiveFeedEvent upsert(LiveFeedCard card);

    /** Removes the live's card; empty — no version bump, nothing published — when it isn't in the feed. */
    Optional<LiveFeedEvent> remove(UUID liveId);

    LiveFeedSnapshot snapshot();

    /**
     * Current snapshot version; {@code 0} when the snapshot has never been built. Versions never
     * regress: a rebuild, or the first write on an empty (e.g. flushed) snapshot, seeds the
     * version to at least the current epoch millis.
     */
    long currentVersion();

    /** Replaces the whole snapshot with {@code cards} and publishes a {@link LiveFeedEvent.Resynced}. */
    LiveFeedEvent replaceAll(List<LiveFeedCard> cards);

    /**
     * Claims the drift-reconciliation lease for {@code ttl} so only one instance reconciles per
     * cycle; {@code false} when another instance holds it. The lease is never released early —
     * it simply expires.
     */
    boolean tryAcquireReconcileLock(Duration ttl);
}
