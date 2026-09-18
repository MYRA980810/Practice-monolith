package com.livecomerce.cart.api;

import com.livecomerce.cart.application.port.in.GetCombinedCartViewUseCase.StoreCartView;

import java.util.List;
import java.util.UUID;

record StoreCartResponse(UUID storeId, List<CartLineResponse> lines) {

    static StoreCartResponse from(StoreCartView view) {
        return new StoreCartResponse(
                view.storeId(),
                view.lines().stream().map(CartLineResponse::from).toList());
    }
}
