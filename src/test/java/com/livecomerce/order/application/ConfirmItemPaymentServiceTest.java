package com.livecomerce.order.application;

import com.livecomerce.order.application.port.in.ConfirmItemPaymentUseCase.ConfirmItemPaymentCommand;
import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.application.port.out.SaveOrderPort;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemType;
import com.livecomerce.order.OrderItemPaidEvent;
import com.livecomerce.order.domain.OrderItemStatus;
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
class ConfirmItemPaymentServiceTest {

    @Mock LoadOrderPort loadOrderPort;
    @Mock SaveOrderPort saveOrderPort;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks ConfirmItemPaymentService service;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    private Order buildOrderWithReservedItem() {
        var order = Order.open(BUYER_ID, STORE_ID, null, "MXN");
        order.addItem(UUID.randomUUID(), UUID.randomUUID(), "Playera", new BigDecimal("199"), "MXN", 2, 10, OrderItemType.PRODUCT);
        return order;
    }

    @Test
    void confirmPayment_transitionsItemFromReservedToPaid() {
        var order = buildOrderWithReservedItem();
        var itemId = order.getItems().getFirst().getId();
        when(loadOrderPort.loadById(ORDER_ID)).thenReturn(Optional.of(order));
        when(saveOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.confirmPayment(new ConfirmItemPaymentCommand(ORDER_ID, itemId));

        var item = result.getItems().getFirst();
        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.PAID);
        assertThat(item.getPaidAt()).isNotNull();
        assertThat(item.getReservedUntil()).isNull();
    }

    @Test
    @SuppressWarnings("null") // Mockito capture() is @Nullable; publishEvent takes @NonNull — false positive
    void confirmPayment_publishesOrderItemPaidEvent() {
        var order = buildOrderWithReservedItem();
        var itemId = order.getItems().getFirst().getId();
        var productId = order.getItems().getFirst().getProductId();
        when(loadOrderPort.loadById(ORDER_ID)).thenReturn(Optional.of(order));
        when(saveOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.confirmPayment(new ConfirmItemPaymentCommand(ORDER_ID, itemId));

        var captor = ArgumentCaptor.forClass(OrderItemPaidEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().itemId()).isEqualTo(itemId);
        assertThat(captor.getValue().productId()).isEqualTo(productId);
        assertThat(captor.getValue().quantity()).isEqualTo(2);
    }

    @Test
    void confirmPayment_whenOrderNotFound_throwsOrderNotFoundException() {
        when(loadOrderPort.loadById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmPayment(new ConfirmItemPaymentCommand(ORDER_ID, UUID.randomUUID())))
                .isInstanceOf(OrderNotFoundException.class);

        verify(saveOrderPort, never()).save(any());
    }

    @Test
    void confirmPayment_whenItemNotFound_throwsOrderItemNotFoundException() {
        var order = buildOrderWithReservedItem();
        var nonExistentItemId = UUID.randomUUID();
        when(loadOrderPort.loadById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.confirmPayment(new ConfirmItemPaymentCommand(ORDER_ID, nonExistentItemId)))
                .isInstanceOf(OrderItemNotFoundException.class);
    }
}
