package com.livecomerce.live.application;

import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

/** Application-level card for a scheduled live, built by {@link LiveFeedCardAssembler}. */
public record LiveUpcomingCard(
        UUID id,
        UUID sellerId,
        @Nullable UUID storeId,
        String title,
        @Nullable String sellerName,
        @Nullable String thumbnailUrl,
        Instant scheduledAt,
        @Nullable UUID categoryId
) {}
