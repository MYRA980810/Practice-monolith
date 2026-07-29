package com.livecomerce.payment.application.port.in;

import java.util.UUID;

public interface DeleteBuyerPaymentMethodUseCase {

    void delete(UUID userId, UUID paymentMethodId);
}
