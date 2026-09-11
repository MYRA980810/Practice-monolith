package com.livecomerce.analytics.application.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface GetStoreRankingUseCase {

    record StoreRankingView(UUID storeId, int rank, BigDecimal score, BigDecimal avgRating,
                             int reviewCount, long followerCount, BigDecimal recentSalesVolume,
                             OffsetDateTime computedAt) {}

    Page<StoreRankingView> getLatestRanking(Pageable pageable);
}
