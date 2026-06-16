package com.livecomerce.order.application;

import com.livecomerce.order.application.port.in.PlaceOrderItemUseCase.PlaceOrderItemCommand;
import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.application.port.out.SaveOrderPort;
import com.livecomerce.order.StockReservationPort;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceOrderItemServiceTest {

    @Mock LoadOrderPort loadOrderPort;
    @Mock SaveOrderPort saveOrderPort;
    @Mock StockReservationPort stockReservationPort;

    @InjectMocks PlaceOrderItemService service;

    private static final UUID BUYER_ID    = UUID.randomUUID();
    private static final UUID STORE_ID    = UUID.randomUUID();
    private static final UUID LIVE_ID     = UUID.randomUUID();
    private static final UUID PRODUCT_ID  = UUID.randomUUID();
    private static final UUID VARIANT_ID  = UUID.randomUUID();

    private static final PlaceOrderItemCommand VALID_COMMAND = new PlaceOrderItemCommand(
            BUYER_ID, STORE_ID, LIVE_ID, PRODUCT_ID, VARIANT_ID,
            "Playera Roja", new BigDecimal("199.00"), "MXN", 1
    );

    @BeforeEach
    @SuppressWarnings("null")
    void setTtl() {
        ReflectionTestUtils.setField(service, "ttlMinutes", 10);
    }

    @Test
    void placeItem_whenNoActiveOrder_createsNewOrderWithItem() {
        var saved = Order.open(BUYER_ID, STORE_ID, LIVE_ID, "MXN");
        when(loadOrderPort.loadActiveByBuyerAndLive(BUYER_ID, LIVE_ID)).thenReturn(Optional.empty());
        when(saveOrderPort.save(any())).thenReturn(saved);

        service.placeItem(VALID_COMMAND);

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(saveOrderPort).save(captor.capture());
        assertThat(captor.getValue().getBuyerId()).isEqualTo(BUYER_ID);
        assertThat(captor.getValue().getStoreId()).isEqualTo(STORE_ID);
    }

    @Test
    void placeItem_whenActiveOrderExists_addsItemToExistingOrder() {
        var existing = Order.open(BUYER_ID, STORE_ID, LIVE_ID, "MXN");
        when(loadOrderPort.loadActiveByBuyerAndLive(BUYER_ID, LIVE_ID)).thenReturn(Optional.of(existing));
        when(saveOrderPort.save(any())).thenReturn(existing);

        service.placeItem(VALID_COMMAND);

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(saveOrderPort).save(captor.capture());
        assertThat(captor.getValue().getItems()).hasSize(1);
        assertThat(captor.getValue().getItems().getFirst().getProductId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    void placeItem_newItem_hasReservedStatusWithTtl() {
        var existing = Order.open(BUYER_ID, STORE_ID, LIVE_ID, "MXN");
        when(loadOrderPort.loadActiveByBuyerAndLive(BUYER_ID, LIVE_ID)).thenReturn(Optional.of(existing));
        when(saveOrderPort.save(any())).thenReturn(existing);

        service.placeItem(VALID_COMMAND);

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(saveOrderPort).save(captor.capture());
        var item = captor.getValue().getItems().getFirst();
        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.RESERVED);
        assertThat(item.getReservedUntil()).isNotNull();
    }
}
