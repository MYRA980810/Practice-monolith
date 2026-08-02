package com.livecomerce.payment.api;

import com.livecomerce.payment.application.port.in.ListSellerPayoutsUseCase;

import java.time.OffsetDateTime;

public record PayoutItemResponse(String id, long amount, String currency, String status, String method, OffsetDateTime arrivalDate) {
    public static PayoutItemResponse from(ListSellerPayoutsUseCase.PayoutItemResult result) {
        return new PayoutItemResponse(
                result.id(),
                result.amount(),
                result.currency(),
                result.status(),
                result.method(),
                result.arrivalDate()
        );
    }
}
