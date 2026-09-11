package com.livecomerce.order.application.port.in;

import com.livecomerce.order.domain.Order;

import java.util.UUID;

public interface DeliverOrderUseCase {

    Order deliver(DeliverOrderCommand command);

    record DeliverOrderCommand(UUID orderId, UUID buyerId) {}
}
