package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.CreateConnectOnboardingLinkUseCase;
import com.livecomerce.payment.application.port.out.StripeConnectGatewayPort;
import com.livecomerce.payment.infrastructure.config.PaymentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateConnectOnboardingLinkService implements CreateConnectOnboardingLinkUseCase {

    private final SellerConnectAccountResolver sellerConnectAccountResolver;
    private final StripeConnectGatewayPort stripeConnectGatewayPort;
    private final PaymentProperties paymentProperties;

    @Override
    public OnboardingLinkResult createOnboardingLink(CreateOnboardingLinkCommand cmd) {
        var stripeAccountId = sellerConnectAccountResolver.getOrCreateStripeAccountId(cmd.sellerId(), cmd.email());

        var connectConfig = paymentProperties.stripe().connect();
        var url = stripeConnectGatewayPort.createAccountLink(
                stripeAccountId,
                connectConfig.refreshUrl(),
                connectConfig.returnUrl()
        );

        return new OnboardingLinkResult(url);
    }
}
