package com.livecomerce.order.application;

import com.livecomerce.order.LiveOrderFinalizePort;
import com.livecomerce.order.application.port.in.FinalizeOrderUseCase;
import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class LiveOrderFinalizeAdapter implements LiveOrderFinalizePort {

    private final FinalizeOrderUseCase finalizeOrderUseCase;
    private final LoadOrderPort        loadOrderPort;

    @Override
    public Order finalize(FinalizeCommand cmd) {
        return finalizeOrderUseCase.finalizeOrder(
                new FinalizeOrderUseCase.FinalizeOrderCommand(
                        cmd.orderId(), cmd.buyerId(), cmd.shippingAddress()));
    }

    @Override
    public Optional<Order> loadActiveOrderForBuyerAndLive(UUID buyerId, UUID liveSessionId) {
        return loadOrderPort.loadActiveByBuyerAndLive(buyerId, liveSessionId);
    }
}
