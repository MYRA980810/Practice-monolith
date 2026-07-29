package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.CreateSetupIntentUseCase.CreateSetupIntentCommand;
import com.livecomerce.payment.application.port.out.BuyerPaymentMethodPort;
import com.livecomerce.payment.application.port.out.BuyerStripeCustomerPort;
import com.livecomerce.payment.application.port.out.StripeCustomerGatewayPort;
import com.livecomerce.payment.application.port.out.StripePaymentMethodGatewayPort;
import com.livecomerce.payment.application.port.out.StripePaymentMethodGatewayPort.SetupIntentHandle;
import com.livecomerce.payment.domain.BuyerStripeCustomer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateSetupIntentServiceTest {

    @Mock BuyerStripeCustomerPort buyerStripeCustomerPort;
    @Mock BuyerPaymentMethodPort buyerPaymentMethodPort;
    @Mock StripeCustomerGatewayPort stripeCustomerGatewayPort;
    @Mock StripePaymentMethodGatewayPort stripePaymentMethodGatewayPort;
    @InjectMocks CreateSetupIntentService sut;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final String EMAIL = "buyer@example.com";

    @Test
    void createSetupIntent_whenNoStripeCustomerExists_createsAndPersistsCustomerBeforeRequestingSetupIntent() {
        when(buyerStripeCustomerPort.loadByUserId(BUYER_ID)).thenReturn(Optional.empty());
        when(stripeCustomerGatewayPort.createCustomer(EMAIL)).thenReturn("cus_new");
        when(buyerStripeCustomerPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(buyerPaymentMethodPort.countByUserId(BUYER_ID)).thenReturn(0);
        when(stripePaymentMethodGatewayPort.createSetupIntent("cus_new"))
                .thenReturn(new SetupIntentHandle("secret_123", "seti_123"));

        var result = sut.createSetupIntent(new CreateSetupIntentCommand(BUYER_ID, EMAIL));

        var orderVerifier = inOrder(stripeCustomerGatewayPort, buyerStripeCustomerPort, stripePaymentMethodGatewayPort);
        orderVerifier.verify(stripeCustomerGatewayPort).createCustomer(EMAIL);
        var captor = ArgumentCaptor.forClass(BuyerStripeCustomer.class);
        orderVerifier.verify(buyerStripeCustomerPort).save(captor.capture());
        orderVerifier.verify(stripePaymentMethodGatewayPort).createSetupIntent("cus_new");
        assertThat(captor.getValue().getStripeCustomerId()).isEqualTo("cus_new");
        assertThat(result.clientSecret()).isEqualTo("secret_123");
    }

    @Test
    void createSetupIntent_whenStripeCustomerAlreadyExists_neverCreatesANewOneAndReusesId() {
        var existing = BuyerStripeCustomer.create(BUYER_ID, "cus_existing");
        when(buyerStripeCustomerPort.loadByUserId(BUYER_ID)).thenReturn(Optional.of(existing));
        when(buyerPaymentMethodPort.countByUserId(BUYER_ID)).thenReturn(0);
        when(stripePaymentMethodGatewayPort.createSetupIntent("cus_existing"))
                .thenReturn(new SetupIntentHandle("secret_456", "seti_456"));

        var result = sut.createSetupIntent(new CreateSetupIntentCommand(BUYER_ID, EMAIL));

        verify(stripeCustomerGatewayPort, never()).createCustomer(any());
        verify(buyerStripeCustomerPort, never()).save(any());
        verify(stripePaymentMethodGatewayPort).createSetupIntent("cus_existing");
        assertThat(result.clientSecret()).isEqualTo("secret_456");
    }

    @Test
    void createSetupIntent_whenLimitExceeded_throwsBeforeTouchingStripeSetupIntent() {
        var existing = BuyerStripeCustomer.create(BUYER_ID, "cus_existing");
        when(buyerStripeCustomerPort.loadByUserId(BUYER_ID)).thenReturn(Optional.of(existing));
        when(buyerPaymentMethodPort.countByUserId(BUYER_ID)).thenReturn(6);

        assertThatThrownBy(() -> sut.createSetupIntent(new CreateSetupIntentCommand(BUYER_ID, EMAIL)))
                .isInstanceOf(PaymentMethodLimitExceededException.class);

        verify(stripePaymentMethodGatewayPort, never()).createSetupIntent(any());
    }
}
