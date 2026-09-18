package com.livecomerce.cart.api;

import com.livecomerce.cart.application.port.in.AddToCartUseCase.AddToCartResult;

record AddToCartResponse(boolean success, String rejectionReason) {

    static AddToCartResponse from(AddToCartResult result) {
        return new AddToCartResponse(result.success(), result.rejectionReason());
    }
}
