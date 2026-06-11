package com.livecomerce.order.application.port.in;

import com.livecomerce.order.domain.Order;

import java.math.BigDecimal;
import java.util.UUID;

public interface PlaceOrderItemUseCase {

    Order placeItem(PlaceOrderItemCommand command);

    record PlaceOrderItemCommand(
            UUID buyerId,
            UUID storeId,
            UUID liveSessionId,
            UUID productId,
            UUID variantId,
            String productName,
            BigDecimal unitPrice,
            String currency,
            int quantity
    ) {}
}
