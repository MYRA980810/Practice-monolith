package com.livecomerce.store.api;

import com.livecomerce.store.LoadStoreRatingPort.StoreRatingSummary;
import com.livecomerce.store.domain.Store;

public record StoreCardResponse(
        String name,
        String slug,
        String description,
        String logoUrl,
        double averageRating,
        long reviewCount,
        Integer rankingPosition,
        long followerCount
) {
    public static StoreCardResponse from(Store store, StoreRatingSummary rating, Integer rankingPosition, Long followerCount) {
        return new StoreCardResponse(
                store.getName(),
                store.getSlug(),
                store.getDescription(),
                store.getLogoUrl(),
                rating != null ? rating.averageRating() : 0.0,
                rating != null ? rating.reviewCount() : 0L,
                rankingPosition,
                followerCount != null ? followerCount : 0L
        );
    }
}
