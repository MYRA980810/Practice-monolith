package com.livecomerce.live;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code storeId} is nullable (SELLER_PROFILE-context lives have no store),
 * mirroring {@link LiveStartedEvent#storeId()}. {@code occurredAt} is the
 * live's own {@code endedAt} timestamp (not publish time) — see {@link
 * LiveStartedEvent} for why consumers need it for ordering.
 */
public record LiveEndedEvent(UUID liveId, UUID sellerId, UUID storeId, Instant occurredAt) {}
