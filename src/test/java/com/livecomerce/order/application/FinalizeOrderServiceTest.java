package com.livecomerce.order.application;

import com.livecomerce.order.application.port.in.FinalizeOrderUseCase.FinalizeOrderCommand;
import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.application.port.out.SaveOrderPort;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderFinalizedEvent;
import com.livecomerce.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinalizeOrderServiceTest {

    @Mock LoadOrderPort loadOrderPort;
    @Mock SaveOrderPort saveOrderPort;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks FinalizeOrderService service;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID LIVE_ID  = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    private Order buildOpenOrderWithPaidItem() {
        var order = Order.open(BUYER_ID, STORE_ID, LIVE_ID, "MXN");
        order.addItem(UUID.randomUUID(), "Producto", new BigDecimal("100"), "MXN", 1, 10);
        order.confirmItemPayment(order.getItems().getFirst().getId());
        return order;
    }

    private Order buildOpenOrderWithOnlyReservedItems() {
        var order = Order.open(BUYER_ID, STORE_ID, LIVE_ID, "MXN");
        order.addItem(UUID.randomUUID(), "Producto", new BigDecimal("100"), "MXN", 1, 10);
        return order;
    }

    @Test
    void finalize_withPaidItems_transitionsToPending() {
        var order = buildOpenOrderWithPaidItem();
        when(loadOrderPort.loadById(ORDER_ID)).thenReturn(Optional.of(order));
        when(saveOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new FinalizeOrderCommand(ORDER_ID, BUYER_ID, "Calle 123, CDMX");
        var result = service.finalizeOrder(command);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getShippingAddress()).isEqualTo("Calle 123, CDMX");
    }

    @Test
    void finalize_withOnlyReservedItems_transitionsToCancelled() {
        var order = buildOpenOrderWithOnlyReservedItems();
        when(loadOrderPort.loadById(ORDER_ID)).thenReturn(Optional.of(order));
        when(saveOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.finalizeOrder(new FinalizeOrderCommand(ORDER_ID, BUYER_ID, "Calle 123"));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void finalize_publishesOrderFinalizedEvent() {
        var order = buildOpenOrderWithPaidItem();
        when(loadOrderPort.loadById(ORDER_ID)).thenReturn(Optional.of(order));
        when(saveOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.finalizeOrder(new FinalizeOrderCommand(ORDER_ID, BUYER_ID, "Calle 123"));

        @SuppressWarnings("null")
        var captor = ArgumentCaptor.forClass(OrderFinalizedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().buyerId()).isEqualTo(BUYER_ID);
        assertThat(captor.getValue().storeId()).isEqualTo(STORE_ID);
    }

    @Test
    void finalize_whenOrderNotFound_throwsOrderNotFoundException() {
        when(loadOrderPort.loadById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.finalizeOrder(new FinalizeOrderCommand(ORDER_ID, BUYER_ID, "x")))
                .isInstanceOf(OrderNotFoundException.class);

        verify(saveOrderPort, never()).save(any());
    }

    @Test
    void finalize_whenBuyerMismatch_throwsOrderNotOwnedByBuyer() {
        var order = buildOpenOrderWithPaidItem();
        var otherBuyer = UUID.randomUUID();
        when(loadOrderPort.loadById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.finalizeOrder(new FinalizeOrderCommand(ORDER_ID, otherBuyer, "x")))
                .isInstanceOf(OrderNotOwnedByBuyerException.class);
    }

    @Test
    void finalize_whenOrderNotOpen_throwsInvalidOrderState() {
        var order = buildOpenOrderWithPaidItem();
        order.finalizeWith("alguna dirección");
        when(loadOrderPort.loadById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.finalizeOrder(new FinalizeOrderCommand(ORDER_ID, BUYER_ID, "x")))
                .isInstanceOf(InvalidOrderStateException.class);
    }
}
