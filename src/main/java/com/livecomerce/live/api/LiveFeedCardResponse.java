package com.livecomerce.live.api;

import com.livecomerce.live.domain.Live;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public record LiveFeedCardResponse(
        UUID id,
        UUID sellerId,
        @Nullable UUID storeId,
        String title,
        @Nullable String thumbnailUrl,
        long currentViewers,
        Instant startedAt
) {
    public static LiveFeedCardResponse from(Live live, long currentViewers) {
        return new LiveFeedCardResponse(
                live.getId(),
                live.getSellerId(),
                live.getStoreId(),
                live.getTitle(),
                live.getThumbnailUrl(),
                currentViewers,
                live.getStartedAt()
        );
    }
}
