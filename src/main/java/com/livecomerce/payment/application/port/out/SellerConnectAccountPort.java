package com.livecomerce.payment.application.port.out;

import com.livecomerce.payment.domain.SellerConnectAccount;

import java.util.Optional;
import java.util.UUID;

public interface SellerConnectAccountPort {

    Optional<SellerConnectAccount> loadByUserId(UUID userId);

    Optional<SellerConnectAccount> loadByStripeAccountId(String stripeAccountId);

    SellerConnectAccount save(SellerConnectAccount account);
}
