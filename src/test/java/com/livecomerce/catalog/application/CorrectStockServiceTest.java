package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.CorrectStockUseCase.CorrectStockCommand;
import com.livecomerce.catalog.application.port.out.LoadProductVariantPort;
import com.livecomerce.catalog.application.port.out.SaveProductVariantPort;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrectStockServiceTest {

    @Mock LoadProductVariantPort loadProductVariantPort;
    @Mock SaveProductVariantPort saveProductVariantPort;

    @InjectMocks CorrectStockService service;

    private static final UUID STORE_ID   = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();

    private static Product buildProduct() {
        return Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
    }

    @Test
    void correctStock_whenVariantNotFound_throwsProductVariantNotFoundException() {
        when(loadProductVariantPort.loadById(VARIANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.correctStock(new CorrectStockCommand(VARIANT_ID, STORE_ID, 8)))
                .isInstanceOf(ProductVariantNotFoundException.class);
    }

    @Test
    void correctStock_whenStoreDoesNotOwnProduct_throwsAccessDeniedException() {
        var product = buildProduct();
        var variant = product.defaultVariant();
        when(loadProductVariantPort.loadById(VARIANT_ID)).thenReturn(Optional.of(variant));

        var otherStore = UUID.randomUUID();
        assertThatThrownBy(() -> service.correctStock(new CorrectStockCommand(VARIANT_ID, otherStore, 8)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void correctStock_whenOwner_returnsUpdatedVariantView() {
        var product = buildProduct();
        var variant = product.defaultVariant();
        variant.addStock(20);
        when(loadProductVariantPort.loadById(VARIANT_ID)).thenReturn(Optional.of(variant));
        when(saveProductVariantPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.correctStock(new CorrectStockCommand(VARIANT_ID, STORE_ID, 8));

        assertThat(result.stock().availableQuantity()).isEqualTo(8);
        assertThat(result.stock().totalQuantity()).isEqualTo(8);
    }

    @Test
    void correctStock_savesVariantAfterUpdate() {
        var product = buildProduct();
        var variant = product.defaultVariant();
        when(loadProductVariantPort.loadById(VARIANT_ID)).thenReturn(Optional.of(variant));
        when(saveProductVariantPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.correctStock(new CorrectStockCommand(VARIANT_ID, STORE_ID, 5));

        verify(saveProductVariantPort).save(variant);
    }
}
