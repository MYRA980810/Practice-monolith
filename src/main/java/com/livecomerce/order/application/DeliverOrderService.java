package com.livecomerce.order.application;

import com.livecomerce.order.OrderDeliveredEvent;
import com.livecomerce.order.application.port.in.DeliverOrderUseCase;
import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.application.port.out.SaveOrderPort;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliverOrderService implements DeliverOrderUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Order deliver(DeliverOrderCommand command) {
        var order = loadOrderPort.loadById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        if (!order.getBuyerId().equals(command.buyerId())) {
            throw new OrderNotOwnedByBuyerException(command.orderId(), command.buyerId());
        }

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new InvalidOrderStateException(command.orderId(), order.getStatus(), "deliver");
        }

        order.markDelivered();
        var saved = saveOrderPort.save(order);

        eventPublisher.publishEvent(new OrderDeliveredEvent(saved.getId(), saved.getBuyerId(), saved.getStoreId()));

        return saved;
    }
}
