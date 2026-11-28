package com.livecomerce.live.api;

import com.livecomerce.live.application.LiveUpcomingCard;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public record LiveUpcomingCardResponse(
        UUID id,
        UUID sellerId,
        String title,
        @Nullable String sellerName,
        @Nullable String thumbnailUrl,
        Instant scheduledAt,
        @Nullable UUID categoryId
) {
    public static LiveUpcomingCardResponse from(LiveUpcomingCard card) {
        return new LiveUpcomingCardResponse(
                card.id(),
                card.sellerId(),
                card.title(),
                card.sellerName(),
                card.thumbnailUrl(),
                card.scheduledAt(),
                card.categoryId()
        );
    }
}
