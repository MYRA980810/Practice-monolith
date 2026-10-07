package com.livecomerce.live.application;

import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

/** Application-level card for a live currently on air, built by {@link LiveFeedCardAssembler}. */
public record LiveFeedCard(
        UUID id,
        UUID sellerId,
        @Nullable UUID storeId,
        String title,
        @Nullable String sellerName,
        @Nullable String thumbnailUrl,
        long currentViewers,
        Instant startedAt,
        @Nullable UUID categoryId
) {}
