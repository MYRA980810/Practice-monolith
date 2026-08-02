package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.GetSellerPayoutAccountDetailsUseCase;
import com.livecomerce.payment.application.port.out.SellerConnectAccountPort;
import com.livecomerce.payment.application.port.out.StripeConnectGatewayPort;
import com.livecomerce.payment.domain.SellerConnectAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetSellerPayoutAccountDetailsServiceTest {

    @Mock SellerConnectAccountPort sellerConnectAccountPort;
    @Mock StripeConnectGatewayPort stripeConnectGatewayPort;

    GetSellerPayoutAccountDetailsService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sut = new GetSellerPayoutAccountDetailsService(sellerConnectAccountPort, stripeConnectGatewayPort);
    }

    @Test
    void getDetails_whenNoConnectAccountExists_returnsNotConnected() {
        when(sellerConnectAccountPort.loadByUserId(SELLER_ID)).thenReturn(Optional.empty());

        var result = sut.getDetails(SELLER_ID);

        assertThat(result.connected()).isFalse();
        assertThat(result.chargesEnabled()).isFalse();
        assertThat(result.payoutsEnabled()).isFalse();
        assertThat(result.detailsSubmitted()).isFalse();
        assertThat(result.businessName()).isNull();
        assertThat(result.businessUrl()).isNull();
        assertThat(result.email()).isNull();
        assertThat(result.currentlyDue()).isEmpty();
        assertThat(result.pastDue()).isEmpty();
        assertThat(result.disabledReason()).isNull();
        assertThat(result.bankName()).isNull();
        assertThat(result.bankLast4()).isNull();
        assertThat(result.bankAccountStatus()).isNull();
        assertThat(result.balanceAvailable()).isEmpty();
        assertThat(result.balancePending()).isEmpty();
        verify(stripeConnectGatewayPort, never()).retrieveAccountDetails(any());
        verify(stripeConnectGatewayPort, never()).retrieveBalance(any());
    }

    @Test
    void getDetails_whenConnectAccountExists_combinesLocalCapabilitiesWithStripeDetails() {
        var account = SellerConnectAccount.create(SELLER_ID, "acct_existing");
        account.updateCapabilities(true, true, true);
        when(sellerConnectAccountPort.loadByUserId(SELLER_ID)).thenReturn(Optional.of(account));
        when(stripeConnectGatewayPort.retrieveAccountDetails("acct_existing")).thenReturn(
                new StripeConnectGatewayPort.AccountDetails(
                        "Acme Store",
                        "https://acme.example.com",
                        "seller@example.com",
                        List.of("individual.verification.document"),
                        List.of(),
                        null,
                        "STRIPE TEST BANK",
                        "6789",
                        "new"
                ));
        when(stripeConnectGatewayPort.retrieveBalance("acct_existing")).thenReturn(
                new StripeConnectGatewayPort.AccountBalance(
                        List.of(new StripeConnectGatewayPort.BalanceAmount(1000, "usd")),
                        List.of(new StripeConnectGatewayPort.BalanceAmount(500, "usd"))
                ));

        var result = sut.getDetails(SELLER_ID);

        assertThat(result.connected()).isTrue();
        assertThat(result.chargesEnabled()).isTrue();
        assertThat(result.payoutsEnabled()).isTrue();
        assertThat(result.detailsSubmitted()).isTrue();
        assertThat(result.businessName()).isEqualTo("Acme Store");
        assertThat(result.businessUrl()).isEqualTo("https://acme.example.com");
        assertThat(result.email()).isEqualTo("seller@example.com");
        assertThat(result.currentlyDue()).containsExactly("individual.verification.document");
        assertThat(result.pastDue()).isEmpty();
        assertThat(result.disabledReason()).isNull();
        assertThat(result.bankName()).isEqualTo("STRIPE TEST BANK");
        assertThat(result.bankLast4()).isEqualTo("6789");
        assertThat(result.bankAccountStatus()).isEqualTo("new");
        assertThat(result.balanceAvailable()).containsExactly(
                new GetSellerPayoutAccountDetailsUseCase.BalanceAmountResult(1000, "usd"));
        assertThat(result.balancePending()).containsExactly(
                new GetSellerPayoutAccountDetailsUseCase.BalanceAmountResult(500, "usd"));
        verify(stripeConnectGatewayPort, never()).retrieveAccountStatus(any());
    }
}
