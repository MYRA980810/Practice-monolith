package com.livecomerce.cart.api;

import java.util.List;

record CheckoutCartResponse(List<StoreCheckoutResultResponse> results) {

    static CheckoutCartResponse from(com.livecomerce.cart.application.port.in.CheckoutCartUseCase.CheckoutCartResponse response) {
        return new CheckoutCartResponse(response.results().stream().map(StoreCheckoutResultResponse::from).toList());
    }
}
