package com.livecomerce.order.application;

import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.application.port.out.SaveOrderPort;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationExpiryServiceTest {

    @Mock LoadOrderPort loadOrderPort;
    @Mock SaveOrderPort saveOrderPort;

    @InjectMocks ReservationExpiryService service;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();

    @Test
    void expireOverdueReservations_marksExpiredItemsAsExpired() {
        var order = Order.open(BUYER_ID, STORE_ID, null, "MXN");
        order.addItem(UUID.randomUUID(), "Producto", new BigDecimal("99"), "MXN", 1, 10);

        // Manually expire the TTL by simulating past reservedUntil via expireReservedItems directly
        order.expireReservedItems(OffsetDateTime.now().plusMinutes(20));

        when(loadOrderPort.loadOrdersWithExpiredReservations()).thenReturn(List.of(order));
        when(saveOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.expireOverdueReservations();

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(saveOrderPort).save(captor.capture());
        assertThat(captor.getValue().getItems().getFirst().getStatus())
                .isEqualTo(OrderItemStatus.EXPIRED);
    }

    @Test
    void expireOverdueReservations_whenNoExpiredOrders_savesNothing() {
        when(loadOrderPort.loadOrdersWithExpiredReservations()).thenReturn(List.of());

        service.expireOverdueReservations();

        verify(saveOrderPort, never()).save(any());
    }
}
