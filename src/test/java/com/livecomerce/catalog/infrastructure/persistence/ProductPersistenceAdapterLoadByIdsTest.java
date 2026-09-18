package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code loadByIds} exists specifically for {@code
 * catalog.application.CartProductInfoAdapter} to hydrate N cart lines in one
 * round trip. The regression this guards: a naive implementation that calls
 * the repository once per requested id (N+1) instead of once for the whole
 * batch.
 */
@ExtendWith(MockitoExtension.class)
class ProductPersistenceAdapterLoadByIdsTest {

    @Mock ProductJpaRepository repository;

    @InjectMocks ProductPersistenceAdapter adapter;

    @Test
    void loadByIds_withMultipleIds_issuesExactlyOneBulkQuery_notOnePerProduct() {
        var storeId = UUID.randomUUID();
        var productA = Product.create(storeId, "A", null, BigDecimal.TEN, "MXN", "SKU-A", null);
        var productB = Product.create(storeId, "B", null, BigDecimal.TEN, "MXN", "SKU-B", null);
        var productC = Product.create(storeId, "C", null, BigDecimal.TEN, "MXN", "SKU-C", null);
        var ids = List.of(productA.getId(), productB.getId(), productC.getId());

        when(repository.findByIdsWithVariantsAndStock(ids))
                .thenReturn(List.of(productA, productB, productC));

        var result = adapter.loadByIds(ids);

        assertThat(result).containsExactlyInAnyOrder(productA, productB, productC);
        verify(repository, times(1)).findByIdsWithVariantsAndStock(ids);
        verify(repository, times(1)).findByIdsWithImages(ids);
        verify(repository, never()).findByIdWithDetails(any());
    }

    @Test
    void loadByIds_withEmptyCollection_returnsEmptyWithoutQuerying() {
        var result = adapter.loadByIds(List.of());

        assertThat(result).isEmpty();
        verify(repository, never()).findByIdsWithVariantsAndStock(anyList());
        verify(repository, never()).findByIdsWithImages(anyList());
    }

    @Test
    void loadByIds_whenNoProductsMatch_returnsEmptyWithoutHydratingImages() {
        var ids = List.of(UUID.randomUUID());
        when(repository.findByIdsWithVariantsAndStock(ids)).thenReturn(List.of());

        var result = adapter.loadByIds(ids);

        assertThat(result).isEmpty();
        verify(repository, never()).findByIdsWithImages(anyList());
    }
}
