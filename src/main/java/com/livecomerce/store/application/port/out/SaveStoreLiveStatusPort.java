package com.livecomerce.store.application.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Writes the local {@code store_live_status} projection. Used only by {@link
 * com.livecomerce.store.application.StoreLiveStatusEventListener} reacting to
 * {@code live}'s LiveStartedEvent/LiveEndedEvent. {@code occurredAt} is each
 * event's own domain timestamp; both methods key off {@code storeId} (never
 * {@code liveId} alone) so whichever event actually arrives first can create
 * the row and a later out-of-order redelivery of the other is discarded by
 * timestamp comparison instead of silently resurrecting a stale status (see
 * {@code StoreLiveStatusReadEntity}).
 */
public interface SaveStoreLiveStatusPort {

    /** Upserts the store's currently active live, unless this is a stale redelivery. */
    void markLive(UUID storeId, UUID liveId, Instant occurredAt);

    /** Marks the store's live as ended, unless this is a stale redelivery. */
    void markEnded(UUID storeId, UUID liveId, Instant occurredAt);
}
