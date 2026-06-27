package com.livecomerce.order;

import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemType;

import java.math.BigDecimal;
import java.util.UUID;

public interface LivePurchasePort {

    record LivePurchaseCommand(
            UUID buyerId,
            UUID storeId,
            UUID liveSessionId,
            UUID productId,
            UUID variantId,
            String productName,
            BigDecimal unitPrice,
            String currency,
            int quantity,
            OrderItemType itemType
    ) {}

    Order placeItem(LivePurchaseCommand command);
}
