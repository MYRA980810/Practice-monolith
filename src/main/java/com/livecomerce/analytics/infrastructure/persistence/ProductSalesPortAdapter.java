package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.catalog.LoadProductSalesPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@code analytics} already reads {@code order_items} directly via the
 * read-only {@link com.livecomerce.analytics.domain.OrderItemSalesEntity}
 * mapping (see {@code ProductMetricsRepository}'s top-products queries), so
 * it is the module that actually owns per-product sales figures for
 * {@code catalog}'s purposes — not {@code order} itself, which {@code catalog}
 * already depends on for stock reservation and would cycle back if it also
 * implemented a catalog-owned port.
 */
@Component
@RequiredArgsConstructor
class ProductSalesPortAdapter implements LoadProductSalesPort {

    private final ProductMetricsRepository productMetricsRepository;

    @Override
    public Map<UUID, Long> loadSoldCounts(Collection<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : productMetricsRepository.sumUnitsSoldByProductIds(productIds)) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }
}
