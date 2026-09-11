package com.livecomerce.analytics.infrastructure.scheduler;

import com.livecomerce.analytics.LoadStoreRatingPort;
import com.livecomerce.analytics.application.port.out.LoadRecentSalesVolumePort;
import com.livecomerce.analytics.application.port.out.SaveStoreRankingPort;
import com.livecomerce.analytics.domain.StoreRankingSnapshot;
import com.livecomerce.store.application.port.in.GetStoreFollowersUseCase;
import com.livecomerce.store.application.port.in.ListStoresUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Nightly composite ranking of active stores: rating (average per-review
 * "Livento" score) + follower count + recent sales volume, each min-max
 * normalized across the surviving candidate set and combined with
 * configurable weights. Stores below {@code minReviews} are excluded
 * entirely (no row), not zero-scored — a single 5-star review should not
 * outrank an established store with hundreds.
 *
 * Mirrors {@code StaleLiveReconciliationJob}'s per-item try/catch: one
 * store's bad data must not abort ranking for every other store in the
 * same nightly run.
 */
@Component
@RequiredArgsConstructor
class StoreRankingJob {

    private static final Logger log = LoggerFactory.getLogger(StoreRankingJob.class);

    private final ListStoresUseCase listStoresUseCase;
    private final LoadStoreRatingPort loadStoreRatingPort;
    private final GetStoreFollowersUseCase getStoreFollowersUseCase;
    private final LoadRecentSalesVolumePort loadRecentSalesVolumePort;
    private final SaveStoreRankingPort saveStoreRankingPort;

    @Value("${analytics.ranking.min-reviews:3}")
    private int minReviews;

    @Value("${analytics.ranking.recent-sales-window-days:30}")
    private int recentSalesWindowDays;

    @Value("${analytics.ranking.weight-rating:0.5}")
    private double weightRating;

    @Value("${analytics.ranking.weight-followers:0.25}")
    private double weightFollowers;

    @Value("${analytics.ranking.weight-sales:0.25}")
    private double weightSales;

    @Scheduled(cron = "${analytics.ranking.cron:0 30 2 * * *}")
    @Transactional
    void computeRanking() {
        var storeIds = listStoresUseCase.listActiveIds();
        if (storeIds.isEmpty()) {
            return;
        }

        var storeIdSet = Set.copyOf(storeIds);

        Map<UUID, LoadStoreRatingPort.StoreRatingSummary> ratings;
        try {
            ratings = loadStoreRatingPort.loadSummaries(storeIdSet);
        } catch (Exception e) {
            log.error("Failed to load store ratings for {} active stores; ranking will exclude all of them from the rating dimension this run", storeIds.size(), e);
            ratings = Map.of();
        }

        Map<UUID, Long> followers;
        try {
            followers = getStoreFollowersUseCase.getFollowerCounts(storeIdSet);
        } catch (Exception e) {
            log.error("Failed to load follower counts for {} active stores; scoring followers as 0 for all of them this run", storeIds.size(), e);
            followers = Map.of();
        }

        var now = OffsetDateTime.now();
        Map<UUID, BigDecimal> sales;
        try {
            sales = loadRecentSalesVolumePort.loadRecentSalesVolume(
                    storeIdSet, now.minusDays(recentSalesWindowDays), now);
        } catch (Exception e) {
            log.error("Failed to load recent sales volume for {} active stores; scoring sales as 0 for all of them this run", storeIds.size(), e);
            sales = Map.of();
        }

        var candidates = new ArrayList<Candidate>();
        for (var storeId : storeIds) {
            try {
                var rating = ratings.get(storeId);
                if (rating == null || rating.reviewCount() < minReviews) {
                    continue; // excluded, not zero-scored
                }
                candidates.add(new Candidate(
                        storeId,
                        rating.averageRankingImpactScore(),
                        rating.reviewCount(),
                        followers.getOrDefault(storeId, 0L),
                        sales.getOrDefault(storeId, BigDecimal.ZERO)));
            } catch (Exception e) {
                log.warn("Skipping store {} in ranking computation: {}", storeId, e.getMessage());
            }
        }

        log.info("Store ranking: {} of {} active stores ranked (min reviews: {})",
                candidates.size(), storeIds.size(), minReviews);

        saveStoreRankingPort.replaceAll(score(candidates, now));
    }

    private List<StoreRankingSnapshot> score(List<Candidate> candidates, OffsetDateTime computedAt) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        double minFollowers = candidates.stream().mapToLong(Candidate::followerCount).min().orElse(0);
        double maxFollowers = candidates.stream().mapToLong(Candidate::followerCount).max().orElse(0);
        double minSales = candidates.stream().mapToDouble(c -> c.recentSalesVolume().doubleValue()).min().orElse(0.0);
        double maxSales = candidates.stream().mapToDouble(c -> c.recentSalesVolume().doubleValue()).max().orElse(0.0);

        var scored = candidates.stream()
                .map(c -> {
                    double ratingComponent = c.averageRankingImpactScore() / 5.0;
                    double followerComponent = normalize(c.followerCount(), minFollowers, maxFollowers);
                    double salesComponent = normalize(c.recentSalesVolume().doubleValue(), minSales, maxSales);
                    double total = weightRating * ratingComponent
                            + weightFollowers * followerComponent
                            + weightSales * salesComponent;
                    return new Scored(c, total);
                })
                .sorted(Comparator.comparingDouble(Scored::score).reversed()
                        .thenComparing((Scored s) -> s.candidate().reviewCount(), Comparator.reverseOrder())
                        .thenComparing(s -> s.candidate().storeId()))
                .toList();

        var snapshots = new ArrayList<StoreRankingSnapshot>();
        int rank = 1;
        for (var s : scored) {
            snapshots.add(StoreRankingSnapshot.create(
                    s.candidate().storeId(),
                    rank++,
                    BigDecimal.valueOf(s.score()).setScale(6, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(s.candidate().averageRankingImpactScore()).setScale(2, RoundingMode.HALF_UP),
                    (int) s.candidate().reviewCount(),
                    s.candidate().followerCount(),
                    s.candidate().recentSalesVolume(),
                    computedAt));
        }
        return snapshots;
    }

    private static double normalize(double value, double min, double max) {
        return max > min ? (value - min) / (max - min) : 0.5;
    }

    private record Candidate(UUID storeId, double averageRankingImpactScore, long reviewCount,
                              long followerCount, BigDecimal recentSalesVolume) {}

    private record Scored(Candidate candidate, double score) {}
}
