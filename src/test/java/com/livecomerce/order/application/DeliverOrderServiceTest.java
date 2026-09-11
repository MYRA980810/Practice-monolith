package com.livecomerce.order.application;

import com.livecomerce.order.OrderDeliveredEvent;
import com.livecomerce.order.application.port.in.DeliverOrderUseCase.DeliverOrderCommand;
import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.application.port.out.SaveOrderPort;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemType;
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
class DeliverOrderServiceTest {

    @Mock LoadOrderPort loadOrderPort;
    @Mock SaveOrderPort saveOrderPort;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks DeliverOrderService service;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID LIVE_ID  = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    private Order buildShippedOrder() {
        var order = Order.open(BUYER_ID, STORE_ID, LIVE_ID, "MXN");
        var item = order.addItem(UUID.randomUUID(), UUID.randomUUID(), "Producto", new BigDecimal("100"), "MXN", 1, 10, OrderItemType.PRODUCT);
        order.confirmItemPayment(item.getId());
        order.finalizeWith("Calle 123");
        order.markShipped("MX123456789");
        return order;
    }

    @Test
    void deliver_whenShipped_transitionsToDelivered() {
        var order = buildShippedOrder();
        when(loadOrderPort.loadById(ORDER_ID)).thenReturn(Optional.of(order));
        when(saveOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.deliver(new DeliverOrderCommand(ORDER_ID, BUYER_ID));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    @SuppressWarnings("null")
    void deliver_publishesOrderDeliveredEvent() {
        var order = buildShippedOrder();
        when(loadOrderPort.loadById(ORDER_ID)).thenReturn(Optional.of(order));
        when(saveOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deliver(new DeliverOrderCommand(ORDER_ID, BUYER_ID));

        var captor = ArgumentCaptor.forClass(OrderDeliveredEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().buyerId()).isEqualTo(BUYER_ID);
        assertThat(captor.getValue().storeId()).isEqualTo(STORE_ID);
    }

    @Test
    void deliver_whenOrderNotFound_throwsOrderNotFoundException() {
        when(loadOrderPort.loadById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deliver(new DeliverOrderCommand(ORDER_ID, BUYER_ID)))
                .isInstanceOf(OrderNotFoundException.class);

        verify(saveOrderPort, never()).save(any());
    }

    @Test
    void deliver_whenBuyerMismatch_throwsOrderNotOwnedByBuyer() {
        var order = buildShippedOrder();
        var otherBuyer = UUID.randomUUID();
        when(loadOrderPort.loadById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.deliver(new DeliverOrderCommand(ORDER_ID, otherBuyer)))
                .isInstanceOf(OrderNotOwnedByBuyerException.class);
    }

    @Test
    void deliver_whenNotShipped_throwsInvalidOrderState() {
        var order = Order.open(BUYER_ID, STORE_ID, LIVE_ID, "MXN");
        when(loadOrderPort.loadById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.deliver(new DeliverOrderCommand(ORDER_ID, BUYER_ID)))
                .isInstanceOf(InvalidOrderStateException.class);
    }
}
