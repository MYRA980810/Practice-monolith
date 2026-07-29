package com.livecomerce.payment.application.port.out;

import com.livecomerce.payment.domain.BuyerStripeCustomer;

import java.util.Optional;
import java.util.UUID;

public interface BuyerStripeCustomerPort {

    Optional<BuyerStripeCustomer> loadByUserId(UUID userId);

    Optional<BuyerStripeCustomer> loadByStripeCustomerId(String stripeCustomerId);

    BuyerStripeCustomer save(BuyerStripeCustomer customer);
}
