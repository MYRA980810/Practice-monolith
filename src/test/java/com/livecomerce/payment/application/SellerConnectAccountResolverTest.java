package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.out.SellerConnectAccountPort;
import com.livecomerce.payment.application.port.out.StripeConnectGatewayPort;
import com.livecomerce.payment.domain.SellerConnectAccount;
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
class SellerConnectAccountResolverTest {

    @Mock SellerConnectAccountPort sellerConnectAccountPort;
    @Mock StripeConnectGatewayPort stripeConnectGatewayPort;

    SellerConnectAccountResolver sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final String EMAIL = "seller@example.com";

    @BeforeEach
    void setUp() {
        sut = new SellerConnectAccountResolver(sellerConnectAccountPort, stripeConnectGatewayPort);
    }

    @Test
    void getOrCreateStripeAccountId_whenNoConnectAccountExists_createsAndPersistsAccount() {
        when(sellerConnectAccountPort.loadByUserId(SELLER_ID)).thenReturn(Optional.empty());
        when(stripeConnectGatewayPort.createExpressAccount(EMAIL)).thenReturn("acct_new");
        when(sellerConnectAccountPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.getOrCreateStripeAccountId(SELLER_ID, EMAIL);

        assertThat(result).isEqualTo("acct_new");

        var orderVerifier = inOrder(stripeConnectGatewayPort, sellerConnectAccountPort);
        orderVerifier.verify(stripeConnectGatewayPort).createExpressAccount(EMAIL);
        var captor = ArgumentCaptor.forClass(SellerConnectAccount.class);
        orderVerifier.verify(sellerConnectAccountPort).save(captor.capture());
        assertThat(captor.getValue().getStripeAccountId()).isEqualTo("acct_new");
    }

    @Test
    void getOrCreateStripeAccountId_whenConnectAccountAlreadyExists_neverCreatesADuplicateAccount() {
        var existing = SellerConnectAccount.create(SELLER_ID, "acct_existing");
        when(sellerConnectAccountPort.loadByUserId(SELLER_ID)).thenReturn(Optional.of(existing));

        var result = sut.getOrCreateStripeAccountId(SELLER_ID, EMAIL);

        assertThat(result).isEqualTo("acct_existing");
        verify(stripeConnectGatewayPort, never()).createExpressAccount(any());
        verify(sellerConnectAccountPort, never()).save(any());
    }
}
