package com.livecomerce.live;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * {@code storeId} is nullable (SELLER_PROFILE-context lives have no store).
 * {@code wasLive} is {@code true} when the live was LIVE or RECONNECTING right
 * before it was cancelled: consumers that projected a "live now" state on
 * {@link LiveStartedEvent} must clear it just like on {@link LiveEndedEvent}.
 * {@code occurredAt} is the live's own cancellation timestamp (its {@code
 * updatedAt}), not publish time, so it can be ordered against the matching
 * {@link LiveStartedEvent}.
 */
public record LiveCancelledEvent(
        UUID liveId,
        UUID sellerId,
        UUID storeId,
        String title,
        List<UUID> subscriberIds,
        boolean wasLive,
        Instant occurredAt
) {}
