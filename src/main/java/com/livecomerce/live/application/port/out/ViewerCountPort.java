package com.livecomerce.live.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface ViewerCountPort {

    long increment(UUID liveId);

    long decrement(UUID liveId);

    long get(UUID liveId);

    /** Current count of every given live in one round trip; lives without a count map to 0. */
    Map<UUID, Long> getAll(Collection<UUID> liveIds);

    long heartbeat(UUID liveId, String viewerId);

    /**
     * Atomically compares {@code count} against the last broadcast count for
     * this live and records it as the new last-broadcast value. Returns
     * {@code true} only when the count actually changed (or this is the
     * first call for the live) — callers use this to skip redundant
     * RTM broadcasts when the viewer count hasn't moved.
     */
    boolean shouldBroadcast(UUID liveId, long count);
}
