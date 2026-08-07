package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.CreateConnectOnboardingLinkUseCase.CreateOnboardingLinkCommand;
import com.livecomerce.payment.application.port.out.StripeConnectGatewayPort;
import com.livecomerce.payment.infrastructure.config.PaymentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateConnectOnboardingLinkServiceTest {

    private static final String REFRESH_URL = "https://app.example.com/onboarding/refresh";
    private static final String RETURN_URL = "https://app.example.com/onboarding/return";

    @Mock SellerConnectAccountResolver sellerConnectAccountResolver;
    @Mock StripeConnectGatewayPort stripeConnectGatewayPort;

    CreateConnectOnboardingLinkService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final String EMAIL = "seller@example.com";

    @BeforeEach
    void setUp() {
        var paymentProperties = new PaymentProperties(
                new PaymentProperties.StripeProperties(
                        "sk_test",
                        "whsec_test",
                        new PaymentProperties.ConnectProperties(REFRESH_URL, RETURN_URL)
                )
        );
        sut = new CreateConnectOnboardingLinkService(sellerConnectAccountResolver, stripeConnectGatewayPort, paymentProperties);
    }

    @Test
    void createOnboardingLink_delegatesToResolverThenRequestsAccountLink() {
        when(sellerConnectAccountResolver.getOrCreateStripeAccountId(SELLER_ID, EMAIL)).thenReturn("acct_123");
        when(stripeConnectGatewayPort.createAccountLink("acct_123", REFRESH_URL, RETURN_URL))
                .thenReturn("https://connect.stripe.com/link_123");

        var result = sut.createOnboardingLink(new CreateOnboardingLinkCommand(SELLER_ID, EMAIL));

        var orderVerifier = inOrder(sellerConnectAccountResolver, stripeConnectGatewayPort);
        orderVerifier.verify(sellerConnectAccountResolver).getOrCreateStripeAccountId(SELLER_ID, EMAIL);
        orderVerifier.verify(stripeConnectGatewayPort).createAccountLink("acct_123", REFRESH_URL, RETURN_URL);
        assertThat(result.url()).isEqualTo("https://connect.stripe.com/link_123");
    }
}
