package com.livecomerce.live;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a RECONNECTING live's IVS stream comes back within the
 * reconnection window and the live returns to LIVE. {@code storeId} is
 * nullable (SELLER_PROFILE-context lives have no store), mirroring {@link
 * LiveEndedEvent#storeId()}. {@code occurredAt} is the live's own revive
 * timestamp (its {@code updatedAt}), not publish time.
 */
public record LiveRevivedEvent(UUID liveId, UUID sellerId, UUID storeId, Instant occurredAt) {}
