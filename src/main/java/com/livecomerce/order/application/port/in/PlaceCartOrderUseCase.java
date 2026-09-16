package com.livecomerce.order.application.port.in;

import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Batch order placement for a storefront cart checkout: exactly one {@link
 * Order} per store, N items, one save. Deliberately a sibling of {@link
 * PlaceOrderItemUseCase} rather than an extension of it — that use case is
 * the live-purchase hot path ({@code LivePurchaseAdapter} → {@code
 * BuyLiveProductService}) and must stay bit-identical (see design D2).
 */
public interface PlaceCartOrderUseCase {

    Order placeOrder(PlaceCartOrderCommand command);

    record PlaceCartOrderCommand(
            UUID buyerId,
            UUID storeId,
            String currency,
            List<Line> lines
    ) {
        public record Line(
                UUID productId,
                UUID variantId,
                String productName,
                BigDecimal unitPrice,
                int quantity,
                OrderItemType itemType
        ) {}
    }
}
