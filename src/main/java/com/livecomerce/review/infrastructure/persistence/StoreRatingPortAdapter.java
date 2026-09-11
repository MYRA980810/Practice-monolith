package com.livecomerce.review.infrastructure.persistence;

import com.livecomerce.store.LoadStoreRatingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class StoreRatingPortAdapter implements LoadStoreRatingPort {

    private final ReviewJpaRepository reviewJpaRepository;

    @Override
    public Map<UUID, StoreRatingSummary> loadSummaries(Collection<UUID> storeIds) {
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, StoreRatingSummary> summaries = new HashMap<>();
        for (Object[] row : reviewJpaRepository.aggregateByStoreIds(storeIds)) {
            var storeId = (UUID) row[0];
            var avg = toDouble(row[1]);
            var count = ((Number) row[2]).longValue();
            summaries.put(storeId, new StoreRatingSummary(avg, count));
        }
        return summaries;
    }

    private static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof BigDecimal bd) return bd.doubleValue();
        return ((Number) value).doubleValue();
    }
}
