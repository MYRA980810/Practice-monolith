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
class StoreRatingPortAdapterTest {

    @Mock ReviewJpaRepository reviewJpaRepository;

    @InjectMocks StoreRatingPortAdapter adapter;

    @Test
    void loadSummaries_mapsRowsToSummaries() {
        var storeId = UUID.randomUUID();
        when(reviewJpaRepository.aggregateByStoreIds(Set.of(storeId)))
                .thenReturn(List.<Object[]>of(new Object[]{storeId, new BigDecimal("4.30"), 7L}));

        var result = adapter.loadSummaries(Set.of(storeId));

        assertThat(result.get(storeId).averageRating()).isEqualTo(4.30);
        assertThat(result.get(storeId).reviewCount()).isEqualTo(7L);
    }

    @Test
    void loadSummaries_whenStoreHasNoReviews_isAbsentFromResult() {
        var storeId = UUID.randomUUID();
        when(reviewJpaRepository.aggregateByStoreIds(Set.of(storeId))).thenReturn(List.of());

        var result = adapter.loadSummaries(Set.of(storeId));

        assertThat(result).doesNotContainKey(storeId);
    }

    @Test
    void loadSummaries_whenEmptyIds_returnsEmptyMapWithoutQuerying() {
        var result = adapter.loadSummaries(Set.of());

        assertThat(result).isEmpty();
    }
}
