package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.AddStockUseCase.AddStockCommand;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddStockServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock SaveProductPort saveProductPort;

    @InjectMocks AddStockService service;

    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private static Product buildProduct() {
        return Product.create(UUID.randomUUID(), "Remera", null, BigDecimal.TEN, "MXN", null, null);
    }

    @Test
    void addStock_whenProductExists_increasesStock() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.addStock(new AddStockCommand(PRODUCT_ID, 20));

        assertThat(result.getStock().getTotalQuantity()).isEqualTo(20);
        assertThat(result.getStock().getAvailableQuantity()).isEqualTo(20);
    }

    @Test
    void addStock_whenProductNotFound_throwsProductNotFoundException() {
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addStock(new AddStockCommand(PRODUCT_ID, 10)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void addStock_savesProductAfterUpdate() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addStock(new AddStockCommand(PRODUCT_ID, 5));

        verify(saveProductPort).save(product);
    }
}
