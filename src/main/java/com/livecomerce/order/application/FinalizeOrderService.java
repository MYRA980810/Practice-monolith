package com.livecomerce.order.application;

import com.livecomerce.order.application.port.in.FinalizeOrderUseCase;
import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.application.port.out.SaveOrderPort;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderFinalizedEvent;
import com.livecomerce.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FinalizeOrderService implements FinalizeOrderUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Order finalize(FinalizeOrderCommand command) {
        var order = loadOrderPort.loadById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        if (!order.getBuyerId().equals(command.buyerId())) {
            throw new OrderNotOwnedByBuyerException(command.orderId(), command.buyerId());
        }

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new InvalidOrderStateException(command.orderId(), order.getStatus(), "finalize");
        }

        order.finalizeWith(command.shippingAddress());
        var saved = saveOrderPort.save(order);

        eventPublisher.publishEvent(
                new OrderFinalizedEvent(saved.getId(), saved.getBuyerId(), saved.getStoreId(), saved.getStatus()));

        return saved;
    }
}
