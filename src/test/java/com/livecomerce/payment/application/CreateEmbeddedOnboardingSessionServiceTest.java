package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.CreateEmbeddedOnboardingSessionUseCase.CreateEmbeddedOnboardingSessionCommand;
import com.livecomerce.payment.application.port.out.StripeConnectGatewayPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateEmbeddedOnboardingSessionServiceTest {

    @Mock SellerConnectAccountResolver sellerConnectAccountResolver;
    @Mock StripeConnectGatewayPort stripeConnectGatewayPort;

    CreateEmbeddedOnboardingSessionService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final String EMAIL = "seller@example.com";

    @BeforeEach
    void setUp() {
        sut = new CreateEmbeddedOnboardingSessionService(sellerConnectAccountResolver, stripeConnectGatewayPort);
    }

    @Test
    void createEmbeddedOnboardingSession_resolvesAccountThenCreatesSession_returnsClientSecret() {
        when(sellerConnectAccountResolver.getOrCreateStripeAccountId(SELLER_ID, EMAIL)).thenReturn("acct_123");
        when(stripeConnectGatewayPort.createAccountOnboardingSession("acct_123"))
                .thenReturn("accs_test_123_secret_abc");

        var result = sut.createEmbeddedOnboardingSession(new CreateEmbeddedOnboardingSessionCommand(SELLER_ID, EMAIL));

        assertThat(result.clientSecret()).isEqualTo("accs_test_123_secret_abc");

        var orderVerifier = inOrder(sellerConnectAccountResolver, stripeConnectGatewayPort);
        orderVerifier.verify(sellerConnectAccountResolver).getOrCreateStripeAccountId(SELLER_ID, EMAIL);
        orderVerifier.verify(stripeConnectGatewayPort).createAccountOnboardingSession("acct_123");
    }

    @Test
    void createEmbeddedOnboardingSession_passesSellerIdAndEmailToResolver() {
        when(sellerConnectAccountResolver.getOrCreateStripeAccountId(SELLER_ID, EMAIL)).thenReturn("acct_123");
        when(stripeConnectGatewayPort.createAccountOnboardingSession("acct_123"))
                .thenReturn("accs_test_123_secret_abc");

        sut.createEmbeddedOnboardingSession(new CreateEmbeddedOnboardingSessionCommand(SELLER_ID, EMAIL));

        verify(sellerConnectAccountResolver).getOrCreateStripeAccountId(SELLER_ID, EMAIL);
    }
}
