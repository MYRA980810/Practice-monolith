package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.out.LoadProductVariantPort;
import com.livecomerce.catalog.application.port.out.SaveProductVariantPort;
import com.livecomerce.catalog.domain.Product;
import com.livecomerce.catalog.domain.ProductVariant;
import com.livecomerce.order.OrderItemPaidEvent;
import com.livecomerce.order.OrderItemReleasedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockEventListenerTest {

    @Mock LoadProductVariantPort loadProductVariantPort;
    @Mock SaveProductVariantPort saveProductVariantPort;

    @InjectMocks StockEventListener listener;

    private static final UUID ORDER_ID   = UUID.randomUUID();
    private static final UUID ITEM_ID    = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();

    private static ProductVariant buildVariantWithStock(int qty) {
        var product = Product.create(UUID.randomUUID(), "Test", null, BigDecimal.TEN, "MXN", null, null);
        var variant = product.defaultVariant();
        variant.addStock(qty);
        return variant;
    }

    @Test
    void onPaid_whenVariantIdIsNull_doesNotCallLoadVariantPort() {
        listener.on(new OrderItemPaidEvent(ORDER_ID, ITEM_ID, null, null, 2));

        verifyNoInteractions(loadProductVariantPort);
        verifyNoInteractions(saveProductVariantPort);
    }

    @Test
    void onPaid_whenVariantIdIsNotNull_sellsStockAndSaves() {
        var variant = buildVariantWithStock(10);
        variant.reserveStock(3);
        when(loadProductVariantPort.loadById(VARIANT_ID)).thenReturn(Optional.of(variant));

        listener.on(new OrderItemPaidEvent(ORDER_ID, ITEM_ID, PRODUCT_ID, VARIANT_ID, 3));

        assertThat(variant.getStock().getTotalQuantity()).isEqualTo(7);
        verify(saveProductVariantPort).save(variant);
    }

    @Test
    void onReleased_whenVariantIdIsNull_doesNotCallLoadVariantPort() {
        listener.on(new OrderItemReleasedEvent(ORDER_ID, ITEM_ID, null, null, 1));

        verifyNoInteractions(loadProductVariantPort);
        verifyNoInteractions(saveProductVariantPort);
    }

    @Test
    void onReleased_whenVariantIdIsNotNull_releasesStockAndSaves() {
        var variant = buildVariantWithStock(10);
        variant.reserveStock(4);
        when(loadProductVariantPort.loadById(VARIANT_ID)).thenReturn(Optional.of(variant));

        listener.on(new OrderItemReleasedEvent(ORDER_ID, ITEM_ID, PRODUCT_ID, VARIANT_ID, 2));

        assertThat(variant.getStock().getReservedQuantity()).isEqualTo(2);
        assertThat(variant.getStock().getAvailableQuantity()).isEqualTo(8);
        verify(saveProductVariantPort).save(variant);
    }
}
