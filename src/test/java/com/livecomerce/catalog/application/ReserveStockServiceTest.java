package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.ReserveStockUseCase.ReserveStockCommand;
import com.livecomerce.catalog.application.port.out.LoadProductVariantPort;
import com.livecomerce.catalog.application.port.out.SaveProductVariantPort;
import com.livecomerce.catalog.domain.Product;
import com.livecomerce.catalog.domain.ProductVariant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReserveStockServiceTest {

    @Mock LoadProductVariantPort loadProductVariantPort;
    @Mock SaveProductVariantPort saveProductVariantPort;

    @InjectMocks ReserveStockService service;

    private static final UUID VARIANT_ID = UUID.randomUUID();

    private static ProductVariant buildVariant() {
        var product = Product.create(UUID.randomUUID(), "Remera", null, BigDecimal.TEN, "MXN", null, null);
        return product.defaultVariant();
    }

    @Test
    void reserve_whenSufficientStock_reservesAndSaves() {
        var variant = buildVariant();
        variant.addStock(10);
        when(loadProductVariantPort.loadById(VARIANT_ID)).thenReturn(Optional.of(variant));

        service.reserve(new ReserveStockCommand(VARIANT_ID, 5));

        verify(saveProductVariantPort).save(any());
    }

    @Test
    void reserve_whenVariantNotFound_throwsProductVariantNotFoundException() {
        when(loadProductVariantPort.loadById(VARIANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserve(new ReserveStockCommand(VARIANT_ID, 1)))
                .isInstanceOf(ProductVariantNotFoundException.class);

        verify(saveProductVariantPort, never()).save(any());
    }

    @Test
    void reserve_whenInsufficientStock_throwsInsufficientStockException() {
        var variant = buildVariant();
        when(loadProductVariantPort.loadById(VARIANT_ID)).thenReturn(Optional.of(variant));

        assertThatThrownBy(() -> service.reserve(new ReserveStockCommand(VARIANT_ID, 1)))
                .isInstanceOf(InsufficientStockException.class);
    }
}
