package com.livecomerce.order.application;

import com.livecomerce.order.StockReservationPort;
import com.livecomerce.order.application.port.in.PlaceCartOrderUseCase;
import com.livecomerce.order.application.port.out.SaveOrderPort;
import com.livecomerce.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Places exactly one {@link Order} per store checkout, with N items from N
 * input lines, saved once. Unlike {@code PlaceOrderItemService}, this never
 * looks up an existing open order to append to — a cart checkout always
 * opens a fresh Order (design D2: {@code Order.open(buyerId, storeId, null,
 * currency)} is hardcoded, making live-scoping unrepresentable on this
 * path).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PlaceCartOrderService implements PlaceCartOrderUseCase {

    private final SaveOrderPort saveOrderPort;
    private final StockReservationPort stockReservationPort;

    @Value("${order.reservation.ttl-minutes:10}")
    private int ttlMinutes;

    @Override
    public Order placeOrder(PlaceCartOrderCommand command) {
        var order = Order.open(command.buyerId(), command.storeId(), null, command.currency());

        for (var line : command.lines()) {
            if (line.variantId() != null) {
                stockReservationPort.reserve(new StockReservationPort.ReserveStockCommand(
                        line.variantId(), line.quantity()));
            }

            order.addItem(
                    line.productId(),
                    line.variantId(),
                    line.productName(),
                    line.unitPrice(),
                    command.currency(),
                    line.quantity(),
                    ttlMinutes,
                    line.itemType());
        }

        return saveOrderPort.save(order);
    }
}
