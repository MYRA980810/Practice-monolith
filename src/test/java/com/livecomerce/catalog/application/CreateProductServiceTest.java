package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.CreateProductUseCase.CreateProductCommand;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.catalog.domain.Product;
import com.livecomerce.catalog.domain.ProductCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProductServiceTest {

    @Mock SaveProductPort saveProductPort;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks CreateProductService service;

    private static final UUID STORE_ID = UUID.randomUUID();

    private static final CreateProductCommand VALID_COMMAND = new CreateProductCommand(
            STORE_ID, "Remera Básica", "Remera de algodón", new BigDecimal("150.00"), "MXN", "SKU-001", null
    );

    private static Product buildProduct() {
        return Product.create(STORE_ID, "Remera Básica", "Remera de algodón",
                new BigDecimal("150.00"), "MXN", "SKU-001", null);
    }

    @Test
    void create_withValidCommand_savesProductAndReturnsIt() {
        var saved = buildProduct();
        when(saveProductPort.save(any())).thenReturn(saved);

        var result = service.create(VALID_COMMAND);

        assertThat(result.getName()).isEqualTo("Remera Básica");
        assertThat(result.getStoreId()).isEqualTo(STORE_ID);
        assertThat(result.defaultVariant().getSku()).isEqualTo("SKU-001");
    }

    @Test
    @SuppressWarnings("null")
    void create_publishesProductCreatedEvent() {
        var saved = buildProduct();
        when(saveProductPort.save(any())).thenReturn(saved);

        service.create(VALID_COMMAND);

        var captor = ArgumentCaptor.forClass(ProductCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().storeId()).isEqualTo(STORE_ID);
        assertThat(captor.getValue().name()).isEqualTo("Remera Básica");
    }

    @Test
    void create_newProductHasEmptyStock() {
        var saved = buildProduct();
        when(saveProductPort.save(any())).thenReturn(saved);

        service.create(VALID_COMMAND);

        var captor = ArgumentCaptor.forClass(Product.class);
        verify(saveProductPort).save(captor.capture());
        var stock = captor.getValue().defaultVariant().getStock();
        assertThat(stock.getTotalQuantity()).isEqualTo(0);
        assertThat(stock.getAvailableQuantity()).isEqualTo(0);
        assertThat(stock.getReservedQuantity()).isEqualTo(0);
    }

    @Test
    void create_withNullCurrency_defaultsToMXN() {
        var command = new CreateProductCommand(STORE_ID, "Producto", null, BigDecimal.TEN, null, null, null);
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.create(command);

        assertThat(result.getCurrency()).isEqualTo("MXN");
    }
}
