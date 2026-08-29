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
        @Nullable String sellerName,
        @Nullable String thumbnailUrl,
        Instant scheduledAt
) {
    public static LiveUpcomingCardResponse from(Live live, @Nullable String sellerName) {
        return new LiveUpcomingCardResponse(
                live.getId(),
                live.getSellerId(),
                live.getStoreId(),
                live.getTitle(),
                sellerName,
                live.getThumbnailUrl(),
                live.getScheduledAt()
        );
    }
}
