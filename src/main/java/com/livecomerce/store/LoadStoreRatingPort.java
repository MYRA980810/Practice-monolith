package com.livecomerce.store;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface LoadStoreRatingPort {

    record StoreRatingSummary(double averageRating, long reviewCount) {}

    Map<UUID, StoreRatingSummary> loadSummaries(Collection<UUID> storeIds);
}
