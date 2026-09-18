package com.livecomerce.store.api;

import com.livecomerce.store.LoadStoreRatingPort.StoreRatingSummary;
import com.livecomerce.store.domain.Store;

import java.util.UUID;

public record StoreCardResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String logoUrl,
        double averageRating,
        long reviewCount,
        Integer rankingPosition,
        long followerCount,
        boolean liveNow
) {
    public static StoreCardResponse from(Store store, StoreRatingSummary rating, Integer rankingPosition,
                                         Long followerCount, boolean liveNow) {
        return new StoreCardResponse(
                store.getId(),
                store.getName(),
                store.getSlug(),
                store.getDescription(),
                store.getLogoUrl(),
                rating != null ? rating.averageRating() : 0.0,
                rating != null ? rating.reviewCount() : 0L,
                rankingPosition,
                followerCount != null ? followerCount : 0L,
                liveNow
        );
    }
}
