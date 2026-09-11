package com.livecomerce.analytics.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "store_ranking_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreRankingSnapshot {

    @Id
    private UUID id;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(nullable = false)
    private int rank;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal score;

    @Column(name = "avg_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "follower_count", nullable = false)
    private long followerCount;

    @Column(name = "recent_sales_volume", nullable = false, precision = 14, scale = 2)
    private BigDecimal recentSalesVolume;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    public static StoreRankingSnapshot create(UUID storeId, int rank, BigDecimal score, BigDecimal avgRating,
                                               int reviewCount, long followerCount, BigDecimal recentSalesVolume,
                                               OffsetDateTime computedAt) {
        var snapshot = new StoreRankingSnapshot();
        snapshot.id = UUID.randomUUID();
        snapshot.storeId = storeId;
        snapshot.rank = rank;
        snapshot.score = score;
        snapshot.avgRating = avgRating;
        snapshot.reviewCount = reviewCount;
        snapshot.followerCount = followerCount;
        snapshot.recentSalesVolume = recentSalesVolume;
        snapshot.computedAt = computedAt;
        return snapshot;
    }
}
