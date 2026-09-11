package com.livecomerce.review.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductRatingPortAdapterTest {

    @Mock ProductRatingJpaRepository productRatingJpaRepository;

    @InjectMocks ProductRatingPortAdapter adapter;

    @Test
    void loadSummaries_mapsRowsToSummaries() {
        var productId = UUID.randomUUID();
        when(productRatingJpaRepository.aggregateByProductIds(Set.of(productId)))
                .thenReturn(List.<Object[]>of(new Object[]{productId, new BigDecimal("4.80"), 12L}));

        var result = adapter.loadSummaries(Set.of(productId));

        assertThat(result.get(productId).averageRating()).isEqualTo(4.80);
        assertThat(result.get(productId).reviewCount()).isEqualTo(12L);
    }

    @Test
    void loadSummaries_whenProductHasNoRatings_isAbsentFromResult() {
        var productId = UUID.randomUUID();
        when(productRatingJpaRepository.aggregateByProductIds(Set.of(productId))).thenReturn(List.of());

        var result = adapter.loadSummaries(Set.of(productId));

        assertThat(result).doesNotContainKey(productId);
    }

    @Test
    void loadSummaries_whenEmptyIds_returnsEmptyMapWithoutQuerying() {
        var result = adapter.loadSummaries(Set.of());

        assertThat(result).isEmpty();
    }
}
