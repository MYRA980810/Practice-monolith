package com.livecomerce.payment.application;

import com.livecomerce.payment.BuyerPaymentMethodAttachedEvent;
import com.livecomerce.payment.SellerPayoutAccountActivatedEvent;
import com.livecomerce.payment.application.port.in.HandleStripeWebhookUseCase.WebhookCommand;
import com.livecomerce.payment.application.port.out.BuyerPaymentMethodPort;
import com.livecomerce.payment.application.port.out.BuyerStripeCustomerPort;
import com.livecomerce.payment.application.port.out.ProcessedWebhookEventPort;
import com.livecomerce.payment.application.port.out.SellerConnectAccountPort;
import com.livecomerce.payment.application.port.out.StripePaymentMethodGatewayPort;
import com.livecomerce.payment.application.port.out.StripePaymentMethodGatewayPort.PaymentMethodDetails;
import com.livecomerce.payment.application.port.out.StripeWebhookGatewayPort;
import com.livecomerce.payment.application.port.out.StripeWebhookGatewayPort.AccountPayload;
import com.livecomerce.payment.application.port.out.StripeWebhookGatewayPort.ParsedEvent;
import com.livecomerce.payment.application.port.out.StripeWebhookGatewayPort.SetupIntentPayload;
import com.livecomerce.payment.domain.BuyerPaymentMethod;
import com.livecomerce.payment.domain.BuyerStripeCustomer;
import com.livecomerce.payment.domain.SellerConnectAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleStripeWebhookServiceTest {

    @Mock StripeWebhookGatewayPort stripeWebhookGatewayPort;
    @Mock ProcessedWebhookEventPort processedWebhookEventPort;
    @Mock BuyerStripeCustomerPort buyerStripeCustomerPort;
    @Mock BuyerPaymentMethodPort buyerPaymentMethodPort;
    @Mock StripePaymentMethodGatewayPort stripePaymentMethodGatewayPort;
    @Mock SellerConnectAccountPort sellerConnectAccountPort;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks HandleStripeWebhookService sut;

    private static final WebhookCommand COMMAND = new WebhookCommand("payload", "signature");

    @Test
    void handle_whenEventAlreadyProcessed_returnsWithoutTouchingAnyBusinessPort() {
        var parsed = new ParsedEvent("evt_dup", "setup_intent.succeeded", null, null);
        when(stripeWebhookGatewayPort.verifyAndParse("payload", "signature")).thenReturn(parsed);
        when(processedWebhookEventPort.tryMarkProcessed("evt_dup", "setup_intent.succeeded")).thenReturn(false);

        sut.handle(COMMAND);

        verifyNoInteractions(buyerStripeCustomerPort, buyerPaymentMethodPort,
                stripePaymentMethodGatewayPort, sellerConnectAccountPort, eventPublisher);
    }

    @Test
    void handle_setupIntentSucceeded_whenBuyerAtLimit_detachesPaymentMethodAndNeverSaves() {
        var stripeCustomerId = "cus_1";
        var stripePaymentMethodId = "pm_1";
        var parsed = new ParsedEvent("evt_1", "setup_intent.succeeded",
                new SetupIntentPayload(stripeCustomerId, stripePaymentMethodId), null);
        when(stripeWebhookGatewayPort.verifyAndParse("payload", "signature")).thenReturn(parsed);
        when(processedWebhookEventPort.tryMarkProcessed("evt_1", "setup_intent.succeeded")).thenReturn(true);

        var userId = UUID.randomUUID();
        var customer = BuyerStripeCustomer.create(userId, stripeCustomerId);
        when(buyerStripeCustomerPort.loadByStripeCustomerId(stripeCustomerId)).thenReturn(Optional.of(customer));
        when(buyerPaymentMethodPort.countByUserId(userId)).thenReturn(6);

        sut.handle(COMMAND);

        verify(stripePaymentMethodGatewayPort).detachPaymentMethod(stripePaymentMethodId);
        verify(buyerPaymentMethodPort, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void handle_setupIntentSucceeded_forFirstPaymentMethod_savesAsDefaultAndPublishesEvent() {
        var stripeCustomerId = "cus_2";
        var stripePaymentMethodId = "pm_2";
        var parsed = new ParsedEvent("evt_2", "setup_intent.succeeded",
                new SetupIntentPayload(stripeCustomerId, stripePaymentMethodId), null);
        when(stripeWebhookGatewayPort.verifyAndParse("payload", "signature")).thenReturn(parsed);
        when(processedWebhookEventPort.tryMarkProcessed("evt_2", "setup_intent.succeeded")).thenReturn(true);

        var userId = UUID.randomUUID();
        var customer = BuyerStripeCustomer.create(userId, stripeCustomerId);
        when(buyerStripeCustomerPort.loadByStripeCustomerId(stripeCustomerId)).thenReturn(Optional.of(customer));
        when(buyerPaymentMethodPort.countByUserId(userId)).thenReturn(0);
        when(stripePaymentMethodGatewayPort.retrievePaymentMethod(stripePaymentMethodId))
                .thenReturn(new PaymentMethodDetails("visa", "4242", 12, 2030));
        when(buyerPaymentMethodPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.handle(COMMAND);

        var savedCaptor = ArgumentCaptor.forClass(BuyerPaymentMethod.class);
        verify(buyerPaymentMethodPort).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().isDefault()).isTrue();
        assertThat(savedCaptor.getValue().getUserId()).isEqualTo(userId);

        var eventCaptor = ArgumentCaptor.forClass(BuyerPaymentMethodAttachedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(eventCaptor.getValue().buyerPaymentMethodId()).isEqualTo(savedCaptor.getValue().getId());
    }

    @Test
    void handle_accountUpdated_whenActivatingForTheFirstTime_publishesActivationEvent() {
        var stripeAccountId = "acct_1";
        var parsed = new ParsedEvent("evt_3", "account.updated", null,
                new AccountPayload(stripeAccountId, true, true, true));
        when(stripeWebhookGatewayPort.verifyAndParse("payload", "signature")).thenReturn(parsed);
        when(processedWebhookEventPort.tryMarkProcessed("evt_3", "account.updated")).thenReturn(true);

        var userId = UUID.randomUUID();
        var account = SellerConnectAccount.create(userId, stripeAccountId);
        when(sellerConnectAccountPort.loadByStripeAccountId(stripeAccountId)).thenReturn(Optional.of(account));
        when(sellerConnectAccountPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.handle(COMMAND);

        assertThat(account.getActivatedAt()).isNotNull();
        var eventCaptor = ArgumentCaptor.forClass(SellerPayoutAccountActivatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(eventCaptor.getValue().sellerConnectAccountId()).isEqualTo(account.getId());
    }

    @Test
    void handle_accountUpdated_whenAlreadyActive_neverPublishesActivationEventAgain() {
        var stripeAccountId = "acct_2";
        var parsed = new ParsedEvent("evt_4", "account.updated", null,
                new AccountPayload(stripeAccountId, true, true, true));
        when(stripeWebhookGatewayPort.verifyAndParse("payload", "signature")).thenReturn(parsed);
        when(processedWebhookEventPort.tryMarkProcessed("evt_4", "account.updated")).thenReturn(true);

        var userId = UUID.randomUUID();
        var account = SellerConnectAccount.create(userId, stripeAccountId);
        account.updateCapabilities(true, true, true);
        when(sellerConnectAccountPort.loadByStripeAccountId(stripeAccountId)).thenReturn(Optional.of(account));
        when(sellerConnectAccountPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.handle(COMMAND);

        verify(eventPublisher, never()).publishEvent(any(SellerPayoutAccountActivatedEvent.class));
    }
}
