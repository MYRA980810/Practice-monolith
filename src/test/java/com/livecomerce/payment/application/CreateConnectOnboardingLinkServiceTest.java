package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.CreateConnectOnboardingLinkUseCase.CreateOnboardingLinkCommand;
import com.livecomerce.payment.application.port.out.SellerConnectAccountPort;
import com.livecomerce.payment.application.port.out.StripeConnectGatewayPort;
import com.livecomerce.payment.domain.SellerConnectAccount;
import com.livecomerce.payment.infrastructure.config.PaymentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateConnectOnboardingLinkServiceTest {

    private static final String REFRESH_URL = "https://app.example.com/onboarding/refresh";
    private static final String RETURN_URL = "https://app.example.com/onboarding/return";

    @Mock SellerConnectAccountPort sellerConnectAccountPort;
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
        sut = new CreateConnectOnboardingLinkService(sellerConnectAccountPort, stripeConnectGatewayPort, paymentProperties);
    }

    @Test
    void createOnboardingLink_whenNoConnectAccountExists_createsAndPersistsAccountBeforeRequestingLink() {
        when(sellerConnectAccountPort.loadByUserId(SELLER_ID)).thenReturn(Optional.empty());
        when(stripeConnectGatewayPort.createExpressAccount(EMAIL)).thenReturn("acct_new");
        when(sellerConnectAccountPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stripeConnectGatewayPort.createAccountLink("acct_new", REFRESH_URL, RETURN_URL))
                .thenReturn("https://connect.stripe.com/link_new");

        var result = sut.createOnboardingLink(new CreateOnboardingLinkCommand(SELLER_ID, EMAIL));

        var orderVerifier = inOrder(stripeConnectGatewayPort, sellerConnectAccountPort);
        orderVerifier.verify(stripeConnectGatewayPort).createExpressAccount(EMAIL);
        var captor = ArgumentCaptor.forClass(SellerConnectAccount.class);
        orderVerifier.verify(sellerConnectAccountPort).save(captor.capture());
        orderVerifier.verify(stripeConnectGatewayPort).createAccountLink("acct_new", REFRESH_URL, RETURN_URL);
        assertThat(captor.getValue().getStripeAccountId()).isEqualTo("acct_new");
        assertThat(result.url()).isEqualTo("https://connect.stripe.com/link_new");
    }

    @Test
    void createOnboardingLink_whenConnectAccountAlreadyExists_neverCreatesADuplicateAccount() {
        var existing = SellerConnectAccount.create(SELLER_ID, "acct_existing");
        when(sellerConnectAccountPort.loadByUserId(SELLER_ID)).thenReturn(Optional.of(existing));
        when(stripeConnectGatewayPort.createAccountLink("acct_existing", REFRESH_URL, RETURN_URL))
                .thenReturn("https://connect.stripe.com/link_existing");

        var result = sut.createOnboardingLink(new CreateOnboardingLinkCommand(SELLER_ID, EMAIL));

        verify(stripeConnectGatewayPort, never()).createExpressAccount(any());
        verify(sellerConnectAccountPort, never()).save(any());
        verify(stripeConnectGatewayPort).createAccountLink("acct_existing", REFRESH_URL, RETURN_URL);
        assertThat(result.url()).isEqualTo("https://connect.stripe.com/link_existing");
    }
}
