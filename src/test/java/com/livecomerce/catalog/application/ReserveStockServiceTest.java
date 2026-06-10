package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.ReserveStockUseCase.ReserveStockCommand;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReserveStockServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock SaveProductPort saveProductPort;

    @InjectMocks ReserveStockService service;

    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private static Product buildProduct() {
        return Product.create(UUID.randomUUID(), "Remera", null, BigDecimal.TEN, "MXN", null, null);
    }

    @Test
    void reserve_whenSufficientStock_reservesAndSaves() {
        var product = buildProduct();
        product.addStock(10);
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.reserve(new ReserveStockCommand(PRODUCT_ID, 5));

        verify(saveProductPort).save(any());
    }

    @Test
    void reserve_whenProductNotFound_throwsProductNotFoundException() {
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserve(new ReserveStockCommand(PRODUCT_ID, 1)))
                .isInstanceOf(ProductNotFoundException.class);

        verify(saveProductPort, never()).save(any());
    }

    @Test
    void reserve_whenInsufficientStock_throwsInsufficientStockException() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.reserve(new ReserveStockCommand(PRODUCT_ID, 1)))
                .isInstanceOf(InsufficientStockException.class);
    }
}
