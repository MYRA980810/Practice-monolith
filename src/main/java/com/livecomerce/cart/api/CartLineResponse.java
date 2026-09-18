package com.livecomerce.cart.api;

import com.livecomerce.cart.application.port.in.GetCombinedCartViewUseCase.CartLineView;

import java.math.BigDecimal;
import java.util.UUID;

record CartLineResponse(
        UUID productId,
        UUID variantId,
        String name,
        String imageUrl,
        BigDecimal unitPrice,
        String currency,
        int quantity,
        int availableStock,
        String blockedReason
) {

    static CartLineResponse from(CartLineView view) {
        return new CartLineResponse(
                view.productId(),
                view.variantId(),
                view.name(),
                view.imageUrl(),
                view.unitPrice(),
                view.currency(),
                view.quantity(),
                view.availableStock(),
                view.blockedReason());
    }
}
