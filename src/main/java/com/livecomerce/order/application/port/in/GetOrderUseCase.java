package com.livecomerce.order.application.port.in;

import com.livecomerce.order.domain.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GetOrderUseCase {

    Order getById(UUID orderId);

    Optional<Order> getActiveOrder(UUID buyerId, UUID liveSessionId);

    List<Order> getByStore(UUID storeId);
}
