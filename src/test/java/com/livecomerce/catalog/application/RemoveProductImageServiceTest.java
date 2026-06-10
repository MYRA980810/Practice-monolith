package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.RemoveProductImageUseCase.RemoveImageCommand;
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
class RemoveProductImageServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock SaveProductPort saveProductPort;

    @InjectMocks RemoveProductImageService service;

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();

    private static Product buildProduct() {
        return Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
    }

    @Test
    void removeImage_whenValid_removesImageAndReturns() {
        var product = buildProduct();
        product.addImage("https://cdn.example.com/img.jpg", 0, true);
        var imageId = product.getImages().getFirst().getId();

        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.removeImage(new RemoveImageCommand(PRODUCT_ID, STORE_ID, imageId));

        assertThat(result.getImages()).isEmpty();
    }

    @Test
    void removeImage_whenProductNotFound_throwsProductNotFoundException() {
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeImage(
                new RemoveImageCommand(PRODUCT_ID, STORE_ID, UUID.randomUUID())))
                .isInstanceOf(ProductNotFoundException.class);

        verify(saveProductPort, never()).save(any());
    }

    @Test
    void removeImage_whenProductBelongsToDifferentStore_throwsAccessDeniedException() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        var differentStoreId = UUID.randomUUID();

        assertThatThrownBy(() -> service.removeImage(
                new RemoveImageCommand(PRODUCT_ID, differentStoreId, UUID.randomUUID())))
                .isInstanceOf(AccessDeniedException.class);

        verify(saveProductPort, never()).save(any());
    }

    @Test
    void removeImage_whenImageNotFound_throwsProductImageNotFoundException() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        var nonExistentImageId = UUID.randomUUID();

        assertThatThrownBy(() -> service.removeImage(
                new RemoveImageCommand(PRODUCT_ID, STORE_ID, nonExistentImageId)))
                .isInstanceOf(ProductImageNotFoundException.class);
    }
}
