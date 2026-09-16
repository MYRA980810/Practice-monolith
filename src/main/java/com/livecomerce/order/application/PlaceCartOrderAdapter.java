package com.livecomerce.order.application;

import com.livecomerce.order.PlaceCartOrderPort;
import com.livecomerce.order.application.port.in.PlaceCartOrderUseCase;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Implements the cross-module {@link PlaceCartOrderPort} on top of the
 * internal {@link PlaceCartOrderUseCase}. Every {@link CartOrderLine} is a
 * regular storefront purchase, so it is hardcoded to {@link
 * OrderItemType#PRODUCT} here — {@code cart} never needs to know {@code
 * OrderItemType} exists, keeping it out of {@code cart}'s allowed
 * dependencies. The returned {@link PlacedOrder} is self-contained so {@code
 * cart} never needs {@code order.domain.Order} either.
 */
@Component
@RequiredArgsConstructor
class PlaceCartOrderAdapter implements PlaceCartOrderPort {

    private final PlaceCartOrderUseCase placeCartOrderUseCase;

    @Override
    public PlacedOrder placeOrder(PlaceCartOrderCommand command) {
        var lines = command.lines().stream()
                .map(line -> new PlaceCartOrderUseCase.PlaceCartOrderCommand.Line(
                        line.productId(),
                        line.variantId(),
                        line.productName(),
                        line.unitPrice(),
                        line.quantity(),
                        OrderItemType.PRODUCT))
                .toList();

        Order order = placeCartOrderUseCase.placeOrder(new PlaceCartOrderUseCase.PlaceCartOrderCommand(
                command.buyerId(), command.storeId(), command.currency(), lines));

        var total = order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PlacedOrder(order.getId(), total, order.getCurrency());
    }
}
