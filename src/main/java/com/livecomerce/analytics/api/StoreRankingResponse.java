package com.livecomerce.analytics.api;

import com.livecomerce.analytics.application.port.in.GetStoreRankingUseCase.StoreRankingView;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StoreRankingResponse(
        UUID storeId,
        int rank,
        BigDecimal score,
        BigDecimal avgRating,
        int reviewCount,
        long followerCount,
        BigDecimal recentSalesVolume,
        OffsetDateTime computedAt
) {
    public static StoreRankingResponse from(StoreRankingView view) {
        return new StoreRankingResponse(
                view.storeId(), view.rank(), view.score(), view.avgRating(),
                view.reviewCount(), view.followerCount(), view.recentSalesVolume(), view.computedAt());
    }
}
