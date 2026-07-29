package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.CreateConnectOnboardingLinkUseCase;
import com.livecomerce.payment.application.port.out.SellerConnectAccountPort;
import com.livecomerce.payment.application.port.out.StripeConnectGatewayPort;
import com.livecomerce.payment.domain.SellerConnectAccount;
import com.livecomerce.payment.infrastructure.config.PaymentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateConnectOnboardingLinkService implements CreateConnectOnboardingLinkUseCase {

    private final SellerConnectAccountPort sellerConnectAccountPort;
    private final StripeConnectGatewayPort stripeConnectGatewayPort;
    private final PaymentProperties paymentProperties;

    @Override
    public OnboardingLinkResult createOnboardingLink(CreateOnboardingLinkCommand cmd) {
        // Get-or-create is critical here: a seller must never end up with two Connect accounts
        // just because they retried onboarding, so an existing row's stripeAccountId is always reused.
        var stripeAccountId = sellerConnectAccountPort.loadByUserId(cmd.sellerId())
                .map(SellerConnectAccount::getStripeAccountId)
                .orElseGet(() -> {
                    var accountId = stripeConnectGatewayPort.createExpressAccount(cmd.email());
                    sellerConnectAccountPort.save(SellerConnectAccount.create(cmd.sellerId(), accountId));
                    return accountId;
                });

        var connectConfig = paymentProperties.stripe().connect();
        var url = stripeConnectGatewayPort.createAccountLink(
                stripeAccountId,
                connectConfig.refreshUrl(),
                connectConfig.returnUrl()
        );

        return new OnboardingLinkResult(url);
    }
}
