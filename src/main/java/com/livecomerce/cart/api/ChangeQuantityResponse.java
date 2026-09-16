package com.livecomerce.cart.api;

import com.livecomerce.cart.application.port.in.ChangeQuantityUseCase.ChangeQuantityResult;

record ChangeQuantityResponse(
        boolean success,
        String failureReason,
        Integer availableStock,
        int resultingQuantity
) {

    static ChangeQuantityResponse from(ChangeQuantityResult result) {
        return new ChangeQuantityResponse(
                result.success(), result.failureReason(), result.availableStock(), result.resultingQuantity());
    }
}
