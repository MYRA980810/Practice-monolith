package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.PauseProductUseCase;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.catalog.domain.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PauseProductServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock SaveProductPort saveProductPort;

    @InjectMocks PauseProductService service;

    private static final UUID STORE_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private Product activeProduct() {
        return Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
    }

    @Test
    void pause_whenOwnerAndActive_pausesAndSaves() {
        var product = activeProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        service.pause(new PauseProductUseCase.PauseCommand(PRODUCT_ID, STORE_ID));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(saveProductPort).save(captor.capture());
        assertThat(captor.getValue().isPaused()).isTrue();
    }

    @Test
    void pause_whenProductNotFound_throwsProductNotFoundException() {
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pause(new PauseProductUseCase.PauseCommand(PRODUCT_ID, STORE_ID)))
                .isInstanceOf(ProductNotFoundException.class);
        verify(saveProductPort, never()).save(any());
    }

    @Test
    void pause_whenProductBelongsToOtherStore_throwsAccessDeniedException() {
        var product = activeProduct();
        var differentStoreId = UUID.randomUUID();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.pause(new PauseProductUseCase.PauseCommand(PRODUCT_ID, differentStoreId)))
                .isInstanceOf(AccessDeniedException.class);
        verify(saveProductPort, never()).save(any());
    }
}
