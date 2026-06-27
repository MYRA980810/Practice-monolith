package com.livecomerce.order.application;

import com.livecomerce.order.LivePurchasePort;
import com.livecomerce.order.application.port.in.PlaceOrderItemUseCase;
import com.livecomerce.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class LivePurchaseAdapter implements LivePurchasePort {

    private final PlaceOrderItemUseCase placeOrderItemUseCase;

    @Override
    public Order placeItem(LivePurchaseCommand cmd) {
        return placeOrderItemUseCase.placeItem(new PlaceOrderItemUseCase.PlaceOrderItemCommand(
                cmd.buyerId(),
                cmd.storeId(),
                cmd.liveSessionId(),
                cmd.productId(),
                cmd.variantId(),
                cmd.productName(),
                cmd.unitPrice(),
                cmd.currency(),
                cmd.quantity(),
                cmd.itemType()
        ));
    }
}
