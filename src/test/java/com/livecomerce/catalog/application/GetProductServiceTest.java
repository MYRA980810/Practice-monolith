package com.livecomerce.catalog.application;

import com.livecomerce.catalog.LoadProductRatingPort;
import com.livecomerce.catalog.LoadProductRatingPort.ProductRatingSummary;
import com.livecomerce.catalog.application.port.in.ProductFilter;
import com.livecomerce.catalog.application.port.in.ProductFilter.SortBy;
import com.livecomerce.catalog.application.port.in.ProductFilter.StockLevel;
import com.livecomerce.catalog.application.port.out.LoadCategoryPort;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetProductServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock LoadCategoryPort loadCategoryPort;
    @Mock LoadProductRatingPort loadProductRatingPort;

    @InjectMocks GetProductService service;

    private static final UUID STORE_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @BeforeEach
    void defaultNoRatings() {
        when(loadProductRatingPort.loadSummaries(any())).thenReturn(Map.of());
    }

    private static Product buildProduct() {
        return Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
    }

    // --- getById ---

    @Test
    void getById_whenProductExists_returnsView() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        var result = service.getById(PRODUCT_ID);

        assertThat(result.name()).isEqualTo("Remera");
        assertThat(result.storeId()).isEqualTo(STORE_ID);
        assertThat(result.averageRating()).isZero();
        assertThat(result.reviewCount()).isZero();
    }

    @Test
    void getById_whenRatingExists_enrichesView() {
        var product = buildProduct();
        when(loadProductPort.loadById(product.getId())).thenReturn(Optional.of(product));
        when(loadProductRatingPort.loadSummaries(Set.of(product.getId())))
                .thenReturn(Map.of(product.getId(), new ProductRatingSummary(4.5, 10L)));

        var result = service.getById(product.getId());

        assertThat(result.averageRating()).isEqualTo(4.5);
        assertThat(result.reviewCount()).isEqualTo(10L);
    }

    @Test
    void getById_whenProductNotFound_throwsProductNotFoundException() {
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(PRODUCT_ID))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // --- getByStoreId ---

    @Test
    void getByStoreId_returnsAllProductViews() {
        var p1 = buildProduct();
        var p2 = Product.create(STORE_ID, "Pantalón", null, new BigDecimal("200.00"), "MXN", null, null);
        when(loadProductPort.loadByStoreId(STORE_ID)).thenReturn(List.of(p1, p2));

        var result = service.getByStoreId(STORE_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    void getByStoreId_whenNoProducts_returnsEmptyList() {
        when(loadProductPort.loadByStoreId(STORE_ID)).thenReturn(List.of());

        var result = service.getByStoreId(STORE_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void getByStoreId_batchesRatingLookupOncePerList() {
        var p1 = buildProduct();
        var p2 = Product.create(STORE_ID, "Pantalón", null, new BigDecimal("200.00"), "MXN", null, null);
        when(loadProductPort.loadByStoreId(STORE_ID)).thenReturn(List.of(p1, p2));

        service.getByStoreId(STORE_ID);

        verify(loadProductRatingPort, times(1)).loadSummaries(any());
    }

    // --- listWithFilters ---

    @Test
    void listWithFilters_delegatesFilterToPort() {
        var filter = new ProductFilter(STORE_ID, null, SortBy.PRICE_ASC, StockLevel.ALL);
        var product = buildProduct();
        when(loadProductPort.loadByFilter(filter)).thenReturn(List.of(product));

        var result = service.listWithFilters(filter);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Remera");
    }

    @Test
    void listWithFilters_withCategoryId_passesFilterToPort() {
        var categoryId = UUID.randomUUID();
        var filter = new ProductFilter(STORE_ID, categoryId, SortBy.RECENTLY_ADDED, StockLevel.ALL);
        when(loadProductPort.loadByFilter(filter)).thenReturn(List.of());

        var result = service.listWithFilters(filter);

        assertThat(result).isEmpty();
    }

    @Test
    void listWithFilters_whenEmpty_returnsEmptyList() {
        var filter = new ProductFilter(STORE_ID, null, SortBy.PRICE_DESC, StockLevel.CRITICAL);
        when(loadProductPort.loadByFilter(filter)).thenReturn(List.of());

        var result = service.listWithFilters(filter);

        assertThat(result).isEmpty();
    }
}
