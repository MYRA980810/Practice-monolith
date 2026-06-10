package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.DeactivateProductUseCase.DeactivateCommand;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeactivateProductServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock SaveProductPort saveProductPort;

    @InjectMocks DeactivateProductService service;

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();

    private static Product buildProduct() {
        return Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
    }

    @Test
    void deactivate_whenValid_deactivatesProduct() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        service.deactivate(new DeactivateCommand(PRODUCT_ID, STORE_ID));

        var captor = ArgumentCaptor.forClass(Product.class);
        verify(saveProductPort).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void deactivate_whenProductNotFound_throwsProductNotFoundException() {
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(new DeactivateCommand(PRODUCT_ID, STORE_ID)))
                .isInstanceOf(ProductNotFoundException.class);

        verify(saveProductPort, never()).save(any());
    }

    @Test
    void deactivate_whenProductBelongsToDifferentStore_throwsAccessDeniedException() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        var differentStoreId = UUID.randomUUID();

        assertThatThrownBy(() -> service.deactivate(new DeactivateCommand(PRODUCT_ID, differentStoreId)))
                .isInstanceOf(AccessDeniedException.class);

        verify(saveProductPort, never()).save(any());
    }
}
