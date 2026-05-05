package com.livecomerce.order.application.port.in;

import com.livecomerce.order.domain.Order;

import java.util.UUID;

public interface FinalizeOrderUseCase {

    Order finalizeOrder(FinalizeOrderCommand command);

    record FinalizeOrderCommand(UUID orderId, UUID buyerId, String shippingAddress) {}
}
