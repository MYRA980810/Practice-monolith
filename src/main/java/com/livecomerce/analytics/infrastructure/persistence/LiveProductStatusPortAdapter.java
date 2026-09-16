package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.catalog.LoadLiveProductStatusPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implemented here rather than in {@code live} — see {@link LiveProductReadRepository}'s
 * javadoc for the module cycle that a {@code live}-side implementation would create.
 * {@code analytics} already reads {@code live_products}/{@code lives} directly for its
 * own metrics (see {@code LiveProductReadEntity}/{@code LiveReadEntity}), so it is the
 * safe sink for this cross-module read — same shape and rationale as {@code
 * ProductSalesPortAdapter} for {@code catalog.LoadProductSalesPort}.
 */
@Component
@RequiredArgsConstructor
class LiveProductStatusPortAdapter implements LoadLiveProductStatusPort {

    private final LiveProductReadRepository repository;

    @Override
    public Map<UUID, LiveProductBadge> loadStatuses(Collection<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        Set<UUID> pinnedNow = new HashSet<>();
        Set<UUID> exclusiveToActiveLive = new HashSet<>();

        for (Object[] row : repository.findActiveLiveRowsByProductIds(productIds)) {
            var productId = (UUID) row[0];
            var status = (String) row[1];
            var liveStatus = (String) row[2];

            if ("SOLD".equals(status)) {
                continue;
            }
            exclusiveToActiveLive.add(productId);
            if ("PINNED".equals(status) && "LIVE".equals(liveStatus)) {
                pinnedNow.add(productId);
            }
        }

        Map<UUID, LiveProductBadge> result = new HashMap<>();
        for (UUID productId : exclusiveToActiveLive) {
            result.put(productId, new LiveProductBadge(pinnedNow.contains(productId), true));
        }
        return result;
    }
}
