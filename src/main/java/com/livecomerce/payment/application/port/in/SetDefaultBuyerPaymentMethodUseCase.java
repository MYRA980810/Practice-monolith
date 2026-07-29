package com.livecomerce.payment.application.port.in;

import java.util.UUID;

public interface SetDefaultBuyerPaymentMethodUseCase {

    void setDefault(UUID userId, UUID paymentMethodId);
}
