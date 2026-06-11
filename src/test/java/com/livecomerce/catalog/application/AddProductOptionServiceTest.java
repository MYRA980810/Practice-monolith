package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.AddProductOptionUseCase.AddProductOptionCommand;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.catalog.domain.Product;
import org.springframework.security.access.AccessDeniedException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddProductOptionServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock SaveProductPort saveProductPort;

    @InjectMocks AddProductOptionService service;

    private static final UUID STORE_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private static Product buildProduct() {
        return Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
    }

    @Test
    void addOption_whenValidOwner_addsAndSaves() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.addOption(new AddProductOptionCommand(PRODUCT_ID, STORE_ID, "Color", List.of("Red", "Blue")));

        assertThat(result.getOptions()).hasSize(1);
        assertThat(result.getOptions().getFirst().getName()).isEqualTo("Color");
        verify(saveProductPort).save(product);
    }

    @Test
    void addOption_whenProductNotFound_throwsProductNotFoundException() {
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addOption(new AddProductOptionCommand(PRODUCT_ID, STORE_ID, "Size", List.of("S"))))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void addOption_whenWrongStore_throwsAccessDeniedException() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.addOption(
                new AddProductOptionCommand(PRODUCT_ID, UUID.randomUUID(), "Size", List.of("S"))))
                .isInstanceOf(AccessDeniedException.class);
    }
}
