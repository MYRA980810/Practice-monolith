package com.livecomerce.catalog;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface LoadProductRatingPort {

    record ProductRatingSummary(double averageRating, long reviewCount) {}

    Map<UUID, ProductRatingSummary> loadSummaries(Collection<UUID> productIds);
}
