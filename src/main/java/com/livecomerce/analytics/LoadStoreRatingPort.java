package com.livecomerce.analytics;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Separate copy of {@code store.LoadStoreRatingPort}/{@code catalog.LoadProductRatingPort}'s
 * shape, owned by {@code analytics} for the ranking job. {@code analytics}'s declared
 * {@code allowedDependencies} only permits {@code store::in} (not {@code store}'s root
 * package), so it cannot reuse the other modules' copies directly.
 */
public interface LoadStoreRatingPort {

    record StoreRatingSummary(double averageRankingImpactScore, long reviewCount) {}

    Map<UUID, StoreRatingSummary> loadSummaries(Collection<UUID> storeIds);
}
