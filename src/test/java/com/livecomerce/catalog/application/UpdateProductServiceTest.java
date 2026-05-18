package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.UpdateProductUseCase.UpdateProductCommand;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.catalog.domain.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProductServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock SaveProductPort saveProductPort;

    @InjectMocks UpdateProductService service;

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();

    private static Product buildProduct() {
        return Product.create(STORE_ID, "Original Name", null, BigDecimal.TEN, "MXN", null);
    }

    @Test
    void update_whenValid_updatesFieldsAndReturns() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.update(
                new UpdateProductCommand(PRODUCT_ID, STORE_ID, "New Name", "desc", BigDecimal.valueOf(99), "USD", "SKU-001"));

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getBasePrice()).isEqualByComparingTo(BigDecimal.valueOf(99));
    }

    @Test
    void update_whenProductNotFound_throwsProductNotFoundException() {
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                new UpdateProductCommand(PRODUCT_ID, STORE_ID, "Name", null, BigDecimal.TEN, "MXN", null)))
                .isInstanceOf(ProductNotFoundException.class);

        verify(saveProductPort, never()).save(any());
    }

    @Test
    void update_whenProductBelongsToDifferentStore_throwsAccessDeniedException() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        var differentStoreId = UUID.randomUUID();

        assertThatThrownBy(() -> service.update(
                new UpdateProductCommand(PRODUCT_ID, differentStoreId, "Name", null, BigDecimal.TEN, "MXN", null)))
                .isInstanceOf(AccessDeniedException.class);

        verify(saveProductPort, never()).save(any());
    }
}
