package com.livecomerce.billing.application.port.in;

import com.livecomerce.billing.domain.Gateway;
import com.livecomerce.shared.Plan;

import java.util.UUID;

public interface CreateCheckoutUseCase {

    record CreateCheckoutCommand(
            UUID userId,
            Plan plan,
            Gateway gateway
    ) {}

    record CheckoutResult(String checkoutUrl) {}

    CheckoutResult createCheckout(CreateCheckoutCommand command);
}
