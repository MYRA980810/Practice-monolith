package com.livecomerce.live;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * {@code occurredAt} is the live's own {@code startedAt} timestamp (not
 * publish time) so consumers projecting this event can order it against a
 * later {@link LiveEndedEvent} for the same live even if delivery itself is
 * reordered by an at-least-once retry (see {@code StoreLiveStatusEventListener}).
 */
public record LiveStartedEvent(UUID liveId, UUID storeId, String title, List<UUID> subscriberIds, Instant occurredAt) {}
