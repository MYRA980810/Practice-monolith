package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.UpdateOptionValueSwatchUseCase.UpdateOptionValueSwatchCommand;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.catalog.domain.OptionType;
import com.livecomerce.catalog.domain.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateOptionValueSwatchServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock SaveProductPort saveProductPort;

    @InjectMocks UpdateOptionValueSwatchService service;

    private static final UUID STORE_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private static Product buildProductWithColorOption() {
        var product = Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
        product.addOption("Color", OptionType.COLOR, List.of("Red", "Blue"));
        return product;
    }

    @Test
    void updateSwatch_whenValidOwner_assignsHexAndSaves() {
        var product = buildProductWithColorOption();
        var option = product.getOptions().getFirst();
        var value = option.getValues().getFirst();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateSwatch(
                new UpdateOptionValueSwatchCommand(PRODUCT_ID, STORE_ID, option.getId(), value.getId(), "#FF0000"));

        assertThat(result.getOptions().getFirst().getValues().getFirst().getSwatchHex()).isEqualTo("#FF0000");
    }

    @Test
    void updateSwatch_whenProductNotFound_throwsProductNotFoundException() {
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSwatch(
                new UpdateOptionValueSwatchCommand(PRODUCT_ID, STORE_ID, UUID.randomUUID(), UUID.randomUUID(), "#FF0000")))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void updateSwatch_whenWrongStore_throwsAccessDeniedException() {
        var product = buildProductWithColorOption();
        var option = product.getOptions().getFirst();
        var value = option.getValues().getFirst();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.updateSwatch(
                new UpdateOptionValueSwatchCommand(PRODUCT_ID, UUID.randomUUID(), option.getId(), value.getId(), "#FF0000")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateSwatch_withInvalidHex_throwsIllegalArgument() {
        var product = buildProductWithColorOption();
        var option = product.getOptions().getFirst();
        var value = option.getValues().getFirst();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.updateSwatch(
                new UpdateOptionValueSwatchCommand(PRODUCT_ID, STORE_ID, option.getId(), value.getId(), "red")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
