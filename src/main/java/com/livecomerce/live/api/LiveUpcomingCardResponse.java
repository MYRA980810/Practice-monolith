package com.livecomerce.live.api;

import com.livecomerce.live.domain.Live;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public record LiveUpcomingCardResponse(
        UUID id,
        UUID sellerId,
        @Nullable UUID storeId,
        String title,
        @Nullable String thumbnailUrl,
        Instant scheduledAt
) {
    public static LiveUpcomingCardResponse from(Live live) {
        return new LiveUpcomingCardResponse(
                live.getId(),
                live.getSellerId(),
                live.getStoreId(),
                live.getTitle(),
                live.getThumbnailUrl(),
                live.getScheduledAt()
        );
    }
}
