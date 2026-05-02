package com.livecomerce.order.application.port.in;

import com.livecomerce.order.domain.Order;

import java.util.UUID;

public interface ShipOrderUseCase {

    Order ship(ShipOrderCommand command);

    record ShipOrderCommand(UUID orderId, String trackingNumber) {}
}
