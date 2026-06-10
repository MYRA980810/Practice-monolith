package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.UpdateProductImageUseCase.UpdateImageCommand;
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
class UpdateProductImageServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock SaveProductPort saveProductPort;

    @InjectMocks UpdateProductImageService service;

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();

    private static Product buildProduct() {
        return Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
    }

    @Test
    void updateImage_whenValid_updatesImageAndReturns() {
        var product = buildProduct();
        product.addImage("https://cdn.example.com/old.jpg", 0, true);
        var imageId = product.getImages().getFirst().getId();

        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateImage(
                new UpdateImageCommand(PRODUCT_ID, STORE_ID, imageId, "https://cdn.example.com/new.jpg", 1, false));

        assertThat(result.getImages().getFirst().getUrl()).isEqualTo("https://cdn.example.com/new.jpg");
    }

    @Test
    void updateImage_whenProductNotFound_throwsProductNotFoundException() {
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateImage(
                new UpdateImageCommand(PRODUCT_ID, STORE_ID, UUID.randomUUID(), "https://cdn.example.com/img.jpg", 0, true)))
                .isInstanceOf(ProductNotFoundException.class);

        verify(saveProductPort, never()).save(any());
    }

    @Test
    void updateImage_whenProductBelongsToDifferentStore_throwsAccessDeniedException() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        var differentStoreId = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateImage(
                new UpdateImageCommand(PRODUCT_ID, differentStoreId, UUID.randomUUID(), "https://cdn.example.com/img.jpg", 0, true)))
                .isInstanceOf(AccessDeniedException.class);

        verify(saveProductPort, never()).save(any());
    }

    @Test
    void updateImage_whenImageNotFound_throwsProductImageNotFoundException() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        var nonExistentImageId = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateImage(
                new UpdateImageCommand(PRODUCT_ID, STORE_ID, nonExistentImageId, "https://cdn.example.com/img.jpg", 0, true)))
                .isInstanceOf(ProductImageNotFoundException.class);
    }
}
