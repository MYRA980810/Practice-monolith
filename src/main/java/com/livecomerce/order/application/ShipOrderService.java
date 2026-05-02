package com.livecomerce.order.application;

import com.livecomerce.order.application.port.in.ShipOrderUseCase;
import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.application.port.out.SaveOrderPort;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipOrderService implements ShipOrderUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;

    @Override
    public Order ship(ShipOrderCommand command) {
        var order = loadOrderPort.loadById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new InvalidOrderStateException(command.orderId(), order.getStatus(), "ship");
        }

        order.markShipped(command.trackingNumber());
        return saveOrderPort.save(order);
    }
}
