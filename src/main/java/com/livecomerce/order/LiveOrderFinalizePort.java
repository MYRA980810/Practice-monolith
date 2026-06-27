package com.livecomerce.order;

import com.livecomerce.order.domain.Order;

import java.util.Optional;
import java.util.UUID;

public interface LiveOrderFinalizePort {

    record FinalizeCommand(UUID orderId, UUID buyerId, String shippingAddress) {}

    Order finalize(FinalizeCommand command);

    Optional<Order> loadActiveOrderForBuyerAndLive(UUID buyerId, UUID liveSessionId);
}
