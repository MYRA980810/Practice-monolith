package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.AddProductImagesUseCase.AddImagesCommand;
import com.livecomerce.catalog.application.port.in.AddProductImagesUseCase.ImageData;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddProductImagesServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock SaveProductPort saveProductPort;

    @InjectMocks AddProductImagesService service;

    private static final UUID STORE_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private static Product buildProduct() {
        return Product.create(STORE_ID, "Remera", null, new BigDecimal("100.00"), "MXN", null, null);
    }

    @Test
    void addImages_withExistingImagePlusBatchOfTwo_savedProductHasThreeImages() {
        var product = buildProduct();
        product.addImage("https://cdn.example.com/existing.jpg", 0, true);
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var newImages = List.of(
                new ImageData("https://cdn.example.com/new1.jpg", 1, false),
                new ImageData("https://cdn.example.com/new2.jpg", 2, false)
        );
        var result = service.addImages(new AddImagesCommand(PRODUCT_ID, STORE_ID, newImages));

        assertThat(result.getImages()).hasSize(3);
    }

    @Test
    void addImages_whenProductNotFound_throwsProductNotFoundException() {
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addImages(
                new AddImagesCommand(PRODUCT_ID, STORE_ID, List.of())))
                .isInstanceOf(ProductNotFoundException.class);

        verify(saveProductPort, never()).save(any());
    }

    @Test
    void addImages_whenWrongStoreId_throwsAccessDeniedException() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        var wrongStoreId = UUID.randomUUID();
        assertThatThrownBy(() -> service.addImages(
                new AddImagesCommand(PRODUCT_ID, wrongStoreId, List.of())))
                .isInstanceOf(AccessDeniedException.class);

        verify(saveProductPort, never()).save(any());
    }
}
