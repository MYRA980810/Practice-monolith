package com.livecomerce.payment.application.port.in;

import com.livecomerce.payment.domain.BuyerPaymentMethod;

import java.util.List;
import java.util.UUID;

public interface GetBuyerPaymentMethodsUseCase {

    List<BuyerPaymentMethod> listByUserId(UUID userId);
}
