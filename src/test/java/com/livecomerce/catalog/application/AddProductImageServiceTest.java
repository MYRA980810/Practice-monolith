package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.AddProductImageUseCase.AddImageCommand;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddProductImageServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock SaveProductPort saveProductPort;

    @InjectMocks AddProductImageService service;

    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private static Product buildProduct() {
        return Product.create(UUID.randomUUID(), "Remera", null, BigDecimal.TEN, "MXN", null, null);
    }

    @Test
    void addImage_whenProductExists_addsImageAndSaves() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.addImage(
                new AddImageCommand(PRODUCT_ID, "https://cdn.example.com/img.jpg", 0, true));

        assertThat(result.getImages()).hasSize(1);
        assertThat(result.getImages().getFirst().getUrl()).isEqualTo("https://cdn.example.com/img.jpg");
        assertThat(result.getImages().getFirst().isPrimary()).isTrue();
    }

    @Test
    void addImage_whenProductNotFound_throwsProductNotFoundException() {
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addImage(
                new AddImageCommand(PRODUCT_ID, "https://cdn.example.com/img.jpg", 0, false)))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
