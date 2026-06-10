package com.livecomerce.catalog.application;

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

    @InjectMocks GetProductService service;

    private static final UUID STORE_ID  = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private static Product buildProduct() {
        return Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
    }

    // --- getById ---

    @Test
    void getById_whenProductExists_returnsIt() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        var result = service.getById(PRODUCT_ID);

        assertThat(result.getName()).isEqualTo("Remera");
        assertThat(result.getStoreId()).isEqualTo(STORE_ID);
    }

    @Test
    void getById_whenProductNotFound_throwsProductNotFoundException() {
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(PRODUCT_ID))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // --- getByStoreId ---

    @Test
    void getByStoreId_returnsAllProducts() {
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
}
