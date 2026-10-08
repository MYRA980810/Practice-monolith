package com.livecomerce.live;

import java.util.UUID;

/**
 * Published when a LIVE live is moved to another category, so projections of the
 * active feed (card category, per-category counts) can refresh. Not published for
 * SCHEDULED/RECONNECTING lives: they aren't on the active feed.
 */
public record LiveCategoryChangedEvent(UUID liveId, UUID categoryId) {}
