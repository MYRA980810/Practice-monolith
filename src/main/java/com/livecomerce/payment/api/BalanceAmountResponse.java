package com.livecomerce.payment.api;

import com.livecomerce.payment.application.port.in.GetSellerPayoutAccountDetailsUseCase;

public record BalanceAmountResponse(long amount, String currency) {
    public static BalanceAmountResponse from(GetSellerPayoutAccountDetailsUseCase.BalanceAmountResult result) {
        return new BalanceAmountResponse(result.amount(), result.currency());
    }
}
