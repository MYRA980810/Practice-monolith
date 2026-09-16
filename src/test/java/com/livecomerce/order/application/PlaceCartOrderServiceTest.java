package com.livecomerce.order.application;

import com.livecomerce.order.StockReservationPort;
import com.livecomerce.order.application.port.in.PlaceCartOrderUseCase.PlaceCartOrderCommand;
import com.livecomerce.order.application.port.in.PlaceCartOrderUseCase.PlaceCartOrderCommand.Line;
import com.livecomerce.order.application.port.out.SaveOrderPort;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * D2: a cart checkout always opens a fresh Order per store — there is no
 * "still-open order to reuse" concept outside a live, unlike {@code
 * PlaceOrderItemService}. This is a sibling service, not an extension of
 * {@code PlaceOrderItemService}, so the live-purchase hot path stays
 * bit-identical (see the regression gate task).
 */
@ExtendWith(MockitoExtension.class)
class PlaceCartOrderServiceTest {

    @Mock SaveOrderPort saveOrderPort;
    @Mock StockReservationPort stockReservationPort;

    @InjectMocks PlaceCartOrderService service;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID PRODUCT_1 = UUID.randomUUID();
    private static final UUID VARIANT_1 = UUID.randomUUID();
    private static final UUID PRODUCT_2 = UUID.randomUUID();
    private static final UUID VARIANT_2 = UUID.randomUUID();

    @BeforeEach
    @SuppressWarnings("null")
    void setTtl() {
        ReflectionTestUtils.setField(service, "ttlMinutes", 10);
    }

    @Test
    void placeOrder_opensExactlyOneOrder_withNullLiveSessionId() {
        var command = new PlaceCartOrderCommand(BUYER_ID, STORE_ID, "MXN", java.util.List.of(
                new Line(PRODUCT_1, VARIANT_1, "Playera", new BigDecimal("100.00"), 1, OrderItemType.PRODUCT)
        ));
        when(saveOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.placeOrder(command);

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(saveOrderPort, times(1)).save(captor.capture());
        var order = captor.getValue();
        assertThat(order.getBuyerId()).isEqualTo(BUYER_ID);
        assertThat(order.getStoreId()).isEqualTo(STORE_ID);
        assertThat(order.getLiveSessionId()).isNull();
    }

    @Test
    void placeOrder_addsOneItemPerInputLine() {
        var command = new PlaceCartOrderCommand(BUYER_ID, STORE_ID, "MXN", java.util.List.of(
                new Line(PRODUCT_1, VARIANT_1, "Playera", new BigDecimal("100.00"), 1, OrderItemType.PRODUCT),
                new Line(PRODUCT_2, VARIANT_2, "Pantalón", new BigDecimal("250.00"), 2, OrderItemType.PRODUCT)
        ));
        when(saveOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.placeOrder(command);

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(saveOrderPort).save(captor.capture());
        assertThat(captor.getValue().getItems()).hasSize(2);
        assertThat(captor.getValue().getItems())
                .extracting(item -> item.getProductId())
                .containsExactlyInAnyOrder(PRODUCT_1, PRODUCT_2);
    }

    @Test
    void placeOrder_savesExactlyOnce_regardlessOfLineCount() {
        var command = new PlaceCartOrderCommand(BUYER_ID, STORE_ID, "MXN", java.util.List.of(
                new Line(PRODUCT_1, VARIANT_1, "Playera", new BigDecimal("100.00"), 1, OrderItemType.PRODUCT),
                new Line(PRODUCT_2, VARIANT_2, "Pantalón", new BigDecimal("250.00"), 2, OrderItemType.PRODUCT),
                new Line(UUID.randomUUID(), UUID.randomUUID(), "Gorra", new BigDecimal("50.00"), 1, OrderItemType.PRODUCT)
        ));
        when(saveOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.placeOrder(command);

        verify(saveOrderPort, times(1)).save(any());
    }

    @Test
    void placeOrder_reservesStockOnlyForLinesWithVariantId() {
        var command = new PlaceCartOrderCommand(BUYER_ID, STORE_ID, "MXN", java.util.List.of(
                new Line(PRODUCT_1, VARIANT_1, "Playera", new BigDecimal("100.00"), 1, OrderItemType.PRODUCT),
                new Line(PRODUCT_2, null, "Envío", new BigDecimal("50.00"), 1, OrderItemType.SHIPPING)
        ));
        when(saveOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.placeOrder(command);

        verify(stockReservationPort, times(1)).reserve(any());
        verify(stockReservationPort).reserve(new StockReservationPort.ReserveStockCommand(VARIANT_1, 1));
    }

    @Test
    void placeOrder_withNoVariantIdsAtAll_neverCallsStockReservation() {
        var command = new PlaceCartOrderCommand(BUYER_ID, STORE_ID, "MXN", java.util.List.of(
                new Line(PRODUCT_1, null, "Envío", new BigDecimal("50.00"), 1, OrderItemType.SHIPPING)
        ));
        when(saveOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.placeOrder(command);

        verify(stockReservationPort, never()).reserve(any());
    }
}
