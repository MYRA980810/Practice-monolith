package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.AddStockUseCase.AddStockCommand;
import com.livecomerce.catalog.application.port.out.LoadProductVariantPort;
import com.livecomerce.catalog.application.port.out.SaveProductVariantPort;
import com.livecomerce.catalog.domain.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddStockServiceTest {

    @Mock LoadProductVariantPort loadProductVariantPort;
    @Mock SaveProductVariantPort saveProductVariantPort;

    @InjectMocks AddStockService service;

    private static final UUID VARIANT_ID = UUID.randomUUID();

    private static Product buildProduct() {
        return Product.create(UUID.randomUUID(), "Remera", null, BigDecimal.TEN, "MXN", null, null);
    }

    @Test
    void addStock_whenVariantExists_returnsViewWithUpdatedStock() {
        var product = buildProduct();
        var variant = product.defaultVariant();
        when(loadProductVariantPort.loadById(VARIANT_ID)).thenReturn(Optional.of(variant));
        when(saveProductVariantPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.addStock(new AddStockCommand(VARIANT_ID, 20));

        assertThat(result.stock().totalQuantity()).isEqualTo(20);
        assertThat(result.stock().availableQuantity()).isEqualTo(20);
    }

    @Test
    void addStock_whenVariantNotFound_throwsProductVariantNotFoundException() {
        when(loadProductVariantPort.loadById(VARIANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addStock(new AddStockCommand(VARIANT_ID, 10)))
                .isInstanceOf(ProductVariantNotFoundException.class);
    }

    @Test
    void addStock_savesVariantAfterUpdate() {
        var product = buildProduct();
        var variant = product.defaultVariant();
        when(loadProductVariantPort.loadById(VARIANT_ID)).thenReturn(Optional.of(variant));
        when(saveProductVariantPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addStock(new AddStockCommand(VARIANT_ID, 5));

        verify(saveProductVariantPort).save(variant);
    }
}
