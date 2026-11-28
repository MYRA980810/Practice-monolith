package com.livecomerce.live.api;

import com.livecomerce.live.application.LiveFeedCard;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public record LiveFeedCardResponse(
        UUID id,
        UUID sellerId,
        String title,
        @Nullable String sellerName,
        @Nullable String thumbnailUrl,
        long currentViewers,
        Instant startedAt,
        @Nullable UUID categoryId
) {
    public static LiveFeedCardResponse from(LiveFeedCard card) {
        return new LiveFeedCardResponse(
                card.id(),
                card.sellerId(),
                card.title(),
                card.sellerName(),
                card.thumbnailUrl(),
                card.currentViewers(),
                card.startedAt(),
                card.categoryId()
        );
    }
}
