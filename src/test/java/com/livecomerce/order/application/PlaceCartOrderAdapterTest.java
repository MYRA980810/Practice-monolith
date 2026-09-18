package com.livecomerce.order.application;

import com.livecomerce.order.PlaceCartOrderPort.CartOrderLine;
import com.livecomerce.order.PlaceCartOrderPort.PlaceCartOrderCommand;
import com.livecomerce.order.application.port.in.PlaceCartOrderUseCase;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code cart} must never need {@code order}'s domain types (design D2/D3):
 * this adapter is the only place that touches {@link Order} on the path
 * from {@code cart} into {@code order} — it maps the cross-module command
 * into the internal {@link PlaceCartOrderUseCase} command (hardcoding
 * {@link OrderItemType#PRODUCT}, since the cross-module contract carries no
 * item-type field) and maps the resulting {@link Order} back into a
 * self-contained {@code PlacedOrder}.
 */
@ExtendWith(MockitoExtension.class)
class PlaceCartOrderAdapterTest {

    @Mock PlaceCartOrderUseCase placeCartOrderUseCase;

    @InjectMocks PlaceCartOrderAdapter adapter;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();

    @Test
    void placeOrder_mapsCrossModuleLinesToProductItemType() {
        var command = new PlaceCartOrderCommand(BUYER_ID, STORE_ID, "MXN", List.of(
                new CartOrderLine(PRODUCT_ID, VARIANT_ID, "Playera", new BigDecimal("100.00"), 2)
        ));
        var order = Order.open(BUYER_ID, STORE_ID, null, "MXN");
        order.addItem(PRODUCT_ID, VARIANT_ID, "Playera", new BigDecimal("100.00"), "MXN", 2, 10, OrderItemType.PRODUCT);
        when(placeCartOrderUseCase.placeOrder(any())).thenReturn(order);

        adapter.placeOrder(command);

        var captor = ArgumentCaptor.forClass(PlaceCartOrderUseCase.PlaceCartOrderCommand.class);
        verify(placeCartOrderUseCase).placeOrder(captor.capture());
        var mappedLine = captor.getValue().lines().getFirst();
        assertThat(mappedLine.productId()).isEqualTo(PRODUCT_ID);
        assertThat(mappedLine.variantId()).isEqualTo(VARIANT_ID);
        assertThat(mappedLine.itemType()).isEqualTo(OrderItemType.PRODUCT);
    }

    @Test
    void placeOrder_returnsSelfContainedPlacedOrder_withTotalSummedFromItems() {
        var command = new PlaceCartOrderCommand(BUYER_ID, STORE_ID, "MXN", List.of(
                new CartOrderLine(PRODUCT_ID, VARIANT_ID, "Playera", new BigDecimal("100.00"), 2)
        ));
        var order = Order.open(BUYER_ID, STORE_ID, null, "MXN");
        order.addItem(PRODUCT_ID, VARIANT_ID, "Playera", new BigDecimal("100.00"), "MXN", 2, 10, OrderItemType.PRODUCT);
        when(placeCartOrderUseCase.placeOrder(any())).thenReturn(order);

        var result = adapter.placeOrder(command);

        assertThat(result.orderId()).isEqualTo(order.getId());
        assertThat(result.currency()).isEqualTo("MXN");
        assertThat(result.total()).isEqualByComparingTo("200.00");
    }
}
