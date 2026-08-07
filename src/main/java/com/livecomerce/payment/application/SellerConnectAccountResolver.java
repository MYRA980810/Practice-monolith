package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.out.SellerConnectAccountPort;
import com.livecomerce.payment.application.port.out.StripeConnectGatewayPort;
import com.livecomerce.payment.domain.SellerConnectAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class SellerConnectAccountResolver {

    private final SellerConnectAccountPort sellerConnectAccountPort;
    private final StripeConnectGatewayPort stripeConnectGatewayPort;

    // Get-or-create is critical here: a seller must never end up with two Connect accounts
    // just because they retried onboarding, so an existing row's stripeAccountId is always reused.
    String getOrCreateStripeAccountId(UUID sellerId, String email) {
        return sellerConnectAccountPort.loadByUserId(sellerId)
                .map(SellerConnectAccount::getStripeAccountId)
                .orElseGet(() -> {
                    var accountId = stripeConnectGatewayPort.createExpressAccount(email);
                    sellerConnectAccountPort.save(SellerConnectAccount.create(sellerId, accountId));
                    return accountId;
                });
    }
}
