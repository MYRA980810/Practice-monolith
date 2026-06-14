package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.ProductFilter;
import com.livecomerce.catalog.application.port.in.ProductFilter.SortBy;
import com.livecomerce.catalog.application.port.in.ProductFilter.StockLevel;
import com.livecomerce.catalog.application.port.out.LoadCategoryPort;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.domain.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProductServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock LoadCategoryPort loadCategoryPort;

    @InjectMocks GetProductService service;

    private static final UUID STORE_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

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
