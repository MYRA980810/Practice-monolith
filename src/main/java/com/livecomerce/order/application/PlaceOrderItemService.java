package com.livecomerce.order.application;

import com.livecomerce.order.application.port.in.PlaceOrderItemUseCase;
import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.application.port.out.SaveOrderPort;
import com.livecomerce.order.StockReservationPort;
import com.livecomerce.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PlaceOrderItemService implements PlaceOrderItemUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final StockReservationPort stockReservationPort;

    @Value("${order.reservation.ttl-minutes:10}")
    private int ttlMinutes;

    @Override
    public Order placeItem(PlaceOrderItemCommand command) {
        if (command.variantId() != null) {
            stockReservationPort.reserve(new StockReservationPort.ReserveStockCommand(
                    command.variantId(), command.quantity()));
        }

        var order = loadOrderPort
                .loadActiveByBuyerAndLive(command.buyerId(), command.liveSessionId())
                .orElseGet(() -> Order.open(
                        command.buyerId(),
                        command.storeId(),
                        command.liveSessionId(),
                        command.currency()));

        order.addItem(
                command.productId(),
                command.variantId(),
                command.productName(),
                command.unitPrice(),
                command.currency(),
                command.quantity(),
                ttlMinutes,
                command.itemType());

        return saveOrderPort.save(order);
    }
}
