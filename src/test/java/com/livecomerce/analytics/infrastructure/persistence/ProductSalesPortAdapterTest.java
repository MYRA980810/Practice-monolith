package com.livecomerce.analytics.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSalesPortAdapterTest {

    @Mock ProductMetricsRepository productMetricsRepository;

    @InjectMocks ProductSalesPortAdapter adapter;

    @Test
    void loadSoldCounts_mapsRowsToCounts() {
        var productId = UUID.randomUUID();
        when(productMetricsRepository.sumUnitsSoldByProductIds(Set.of(productId)))
                .thenReturn(List.<Object[]>of(new Object[]{productId, 42L}));

        var result = adapter.loadSoldCounts(Set.of(productId));

        assertThat(result.get(productId)).isEqualTo(42L);
    }

    @Test
    void loadSoldCounts_whenProductNeverSold_isAbsentFromResult() {
        var productId = UUID.randomUUID();
        when(productMetricsRepository.sumUnitsSoldByProductIds(Set.of(productId))).thenReturn(List.of());

        var result = adapter.loadSoldCounts(Set.of(productId));

        assertThat(result).doesNotContainKey(productId);
    }

    @Test
    void loadSoldCounts_whenEmptyIds_returnsEmptyMapWithoutQuerying() {
        var result = adapter.loadSoldCounts(Set.of());

        assertThat(result).isEmpty();
    }
}
