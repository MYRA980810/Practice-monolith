package com.livecomerce.payment.api;

import com.livecomerce.payment.application.port.in.GetSellerPayoutStatusUseCase;

public record PayoutStatusResponse(
        boolean chargesEnabled,
        boolean payoutsEnabled,
        boolean detailsSubmitted,
        boolean connected
) {
    public static PayoutStatusResponse from(GetSellerPayoutStatusUseCase.PayoutStatusResult result) {
        return new PayoutStatusResponse(
                result.chargesEnabled(),
                result.payoutsEnabled(),
                result.detailsSubmitted(),
                result.connected()
        );
    }
}
