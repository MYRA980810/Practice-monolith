package com.livecomerce.order.application;

import com.livecomerce.order.application.port.in.ConfirmItemPaymentUseCase;
import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.application.port.out.SaveOrderPort;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.OrderItemPaidEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ConfirmItemPaymentService implements ConfirmItemPaymentUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Order confirmPayment(ConfirmItemPaymentCommand command) {
        var order = loadOrderPort.loadById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        var item = order.getItems().stream()
                .filter(i -> i.getId().equals(command.itemId()))
                .findFirst()
                .orElseThrow(() -> new OrderItemNotFoundException(command.itemId()));

        order.confirmItemPayment(command.itemId());

        var saved = saveOrderPort.save(order);

        eventPublisher.publishEvent(new OrderItemPaidEvent(
                saved.getId(), item.getId(), item.getProductId(), item.getVariantId(), item.getQuantity()));

        return saved;
    }
}
