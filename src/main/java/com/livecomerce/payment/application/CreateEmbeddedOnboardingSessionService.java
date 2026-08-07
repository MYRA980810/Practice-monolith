package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.CreateEmbeddedOnboardingSessionUseCase;
import com.livecomerce.payment.application.port.out.StripeConnectGatewayPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateEmbeddedOnboardingSessionService implements CreateEmbeddedOnboardingSessionUseCase {

    private final SellerConnectAccountResolver sellerConnectAccountResolver;
    private final StripeConnectGatewayPort stripeConnectGatewayPort;

    @Override
    public EmbeddedOnboardingSessionResult createEmbeddedOnboardingSession(CreateEmbeddedOnboardingSessionCommand cmd) {
        var stripeAccountId = sellerConnectAccountResolver.getOrCreateStripeAccountId(cmd.sellerId(), cmd.email());
        var clientSecret = stripeConnectGatewayPort.createAccountOnboardingSession(stripeAccountId);
        return new EmbeddedOnboardingSessionResult(clientSecret);
    }
}
