package com.livecomerce.cart.api;

import com.livecomerce.cart.application.port.in.CheckoutCartUseCase.StoreCheckoutResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

record StoreCheckoutResultResponse(
        UUID storeId,
        boolean succeeded,
        UUID orderId,
        BigDecimal total,
        String currency,
        List<SkippedLineResponse> skippedLines,
        String failureReason
) {

    static StoreCheckoutResultResponse from(StoreCheckoutResult result) {
        return new StoreCheckoutResultResponse(
                result.storeId(),
                result.succeeded(),
                result.orderId(),
                result.total(),
                result.currency(),
                result.skippedLines().stream().map(SkippedLineResponse::from).toList(),
                result.failureReason());
    }
}
