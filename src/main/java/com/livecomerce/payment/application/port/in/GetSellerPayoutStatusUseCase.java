package com.livecomerce.payment.application.port.in;

import java.util.UUID;

public interface GetSellerPayoutStatusUseCase {

    record PayoutStatusResult(
            boolean chargesEnabled,
            boolean payoutsEnabled,
            boolean detailsSubmitted,
            boolean connected
    ) {
    }

    PayoutStatusResult getStatus(UUID sellerId);
}
