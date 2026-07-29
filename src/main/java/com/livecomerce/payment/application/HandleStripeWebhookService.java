package com.livecomerce.payment.application;

import com.livecomerce.payment.BuyerPaymentMethodAttachedEvent;
import com.livecomerce.payment.SellerPayoutAccountActivatedEvent;
import com.livecomerce.payment.application.port.in.HandleStripeWebhookUseCase;
import com.livecomerce.payment.application.port.out.BuyerPaymentMethodPort;
import com.livecomerce.payment.application.port.out.BuyerStripeCustomerPort;
import com.livecomerce.payment.application.port.out.ProcessedWebhookEventPort;
import com.livecomerce.payment.application.port.out.SellerConnectAccountPort;
import com.livecomerce.payment.application.port.out.StripePaymentMethodGatewayPort;
import com.livecomerce.payment.application.port.out.StripeWebhookGatewayPort;
import com.livecomerce.payment.domain.BuyerPaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HandleStripeWebhookService implements HandleStripeWebhookUseCase {

    private static final String SETUP_INTENT_SUCCEEDED = "setup_intent.succeeded";
    private static final String ACCOUNT_UPDATED = "account.updated";

    private final StripeWebhookGatewayPort stripeWebhookGatewayPort;
    private final ProcessedWebhookEventPort processedWebhookEventPort;
    private final BuyerStripeCustomerPort buyerStripeCustomerPort;
    private final BuyerPaymentMethodPort buyerPaymentMethodPort;
    private final StripePaymentMethodGatewayPort stripePaymentMethodGatewayPort;
    private final SellerConnectAccountPort sellerConnectAccountPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void handle(WebhookCommand cmd) {
        var parsed = stripeWebhookGatewayPort.verifyAndParse(cmd.payload(), cmd.signature());

        if (!processedWebhookEventPort.tryMarkProcessed(parsed.eventId(), parsed.eventType())) {
            log.info("Stripe webhook event {} ({}) already processed, skipping", parsed.eventId(), parsed.eventType());
            return;
        }

        switch (parsed.eventType()) {
            case SETUP_INTENT_SUCCEEDED -> handleSetupIntentSucceeded(parsed);
            case ACCOUNT_UPDATED -> handleAccountUpdated(parsed);
            default -> log.debug("Ignoring unhandled Stripe webhook event type {}", parsed.eventType());
        }
    }

    private void handleSetupIntentSucceeded(StripeWebhookGatewayPort.ParsedEvent parsed) {
        if (parsed.setupIntent() == null) {
            return;
        }

        var stripeCustomerId = parsed.setupIntent().stripeCustomerId();
        var stripePaymentMethodId = parsed.setupIntent().stripePaymentMethodId();

        var customer = buyerStripeCustomerPort.loadByStripeCustomerId(stripeCustomerId).orElse(null);
        if (customer == null) {
            log.warn("Received setup_intent.succeeded for unknown Stripe customer {}", stripeCustomerId);
            return;
        }

        var userId = customer.getUserId();

        if (buyerPaymentMethodPort.countByUserId(userId) >= 6) {
            stripePaymentMethodGatewayPort.detachPaymentMethod(stripePaymentMethodId);
            log.warn("Buyer {} already has the maximum of 6 payment methods; detached {} instead of persisting it",
                    userId, stripePaymentMethodId);
            return;
        }

        var details = stripePaymentMethodGatewayPort.retrievePaymentMethod(stripePaymentMethodId);
        boolean isFirst = buyerPaymentMethodPort.countByUserId(userId) == 0;

        var paymentMethod = BuyerPaymentMethod.create(
                userId,
                stripePaymentMethodId,
                details.brand(),
                details.last4(),
                (short) details.expMonth(),
                (short) details.expYear());

        if (isFirst) {
            paymentMethod.setAsDefault();
        }

        var saved = buyerPaymentMethodPort.save(paymentMethod);
        eventPublisher.publishEvent(new BuyerPaymentMethodAttachedEvent(userId, saved.getId()));
    }

    private void handleAccountUpdated(StripeWebhookGatewayPort.ParsedEvent parsed) {
        if (parsed.account() == null) {
            return;
        }

        var stripeAccountId = parsed.account().stripeAccountId();

        var account = sellerConnectAccountPort.loadByStripeAccountId(stripeAccountId).orElse(null);
        if (account == null) {
            log.warn("Received account.updated for unknown Stripe Connect account {}", stripeAccountId);
            return;
        }

        boolean wasActive = account.getActivatedAt() != null;

        account.updateCapabilities(
                parsed.account().chargesEnabled(),
                parsed.account().payoutsEnabled(),
                parsed.account().detailsSubmitted());

        var saved = sellerConnectAccountPort.save(account);

        if (!wasActive && saved.getActivatedAt() != null) {
            eventPublisher.publishEvent(new SellerPayoutAccountActivatedEvent(saved.getUserId(), saved.getId()));
        }
    }
}
