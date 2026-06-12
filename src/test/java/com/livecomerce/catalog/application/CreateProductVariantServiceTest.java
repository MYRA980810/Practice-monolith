package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.CreateProductVariantUseCase.CreateProductVariantCommand;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProductVariantServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock SaveProductPort saveProductPort;

    @InjectMocks CreateProductVariantService service;

    private static final UUID STORE_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private static Product buildProductWithColorOption() {
        var p = Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
        p.addOption("Color", List.of("Red", "Blue"));
        return p;
    }

    @Test
    void createVariant_whenValid_returnsVariantView() {
        var product = buildProductWithColorOption();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createVariant(new CreateProductVariantCommand(
                PRODUCT_ID, STORE_ID, Map.of("Color", "Red"), "SKU-RED", new BigDecimal("15.00")));

        assertThat(result.isDefault()).isFalse();
        assertThat(result.sku()).isEqualTo("SKU-RED");
        assertThat(result.effectivePrice()).isEqualByComparingTo("15.00");
    }

    @Test
    void createVariant_whenProductNotFound_throwsProductNotFoundException() {
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createVariant(new CreateProductVariantCommand(
                PRODUCT_ID, STORE_ID, Map.of("Color", "Red"), null, null)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void createVariant_whenWrongStore_throwsAccessDeniedException() {
        var product = buildProductWithColorOption();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.createVariant(new CreateProductVariantCommand(
                PRODUCT_ID, UUID.randomUUID(), Map.of("Color", "Red"), null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createVariant_withDuplicateCombination_throwsDuplicateVariantException() {
        var product = buildProductWithColorOption();
        product.addVariant(Map.of("Color", "Red"), "SKU-RED", null);
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.createVariant(new CreateProductVariantCommand(
                PRODUCT_ID, STORE_ID, Map.of("Color", "Red"), "SKU-RED-2", null)))
                .isInstanceOf(DuplicateVariantException.class);
    }
}
