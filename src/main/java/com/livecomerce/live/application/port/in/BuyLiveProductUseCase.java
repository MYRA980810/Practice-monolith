package com.livecomerce.live.application.port.in;

import com.livecomerce.order.domain.Order;

import java.util.UUID;

public interface BuyLiveProductUseCase {

    Order buyLiveProduct(BuyLiveProductCommand command);

    record BuyLiveProductCommand(UUID liveId, UUID liveProductId, UUID buyerId, int quantity) {}
}
