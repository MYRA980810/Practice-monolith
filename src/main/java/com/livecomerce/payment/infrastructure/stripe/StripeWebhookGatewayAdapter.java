package com.livecomerce.payment.infrastructure.stripe;

import com.livecomerce.payment.application.InvalidWebhookSignatureException;
import com.livecomerce.payment.application.port.out.StripeWebhookGatewayPort;
import com.livecomerce.payment.infrastructure.config.PaymentProperties;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Account;
import com.stripe.model.Event;
import com.stripe.model.SetupIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class StripeWebhookGatewayAdapter implements StripeWebhookGatewayPort {

    private final PaymentProperties paymentProperties;

    @Override
    public ParsedEvent verifyAndParse(String payload, String signature) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, paymentProperties.stripe().webhookSecret());
        } catch (SignatureVerificationException e) {
            throw new InvalidWebhookSignatureException("Invalid Stripe webhook signature", e);
        }

        SetupIntentPayload setupIntentPayload = null;
        AccountPayload accountPayload = null;

        if (event.getType().startsWith("setup_intent.")) {
            var stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
            if (stripeObject instanceof SetupIntent setupIntent) {
                setupIntentPayload = new SetupIntentPayload(
                        setupIntent.getCustomer(),
                        setupIntent.getPaymentMethod());
            }
        } else if (event.getType().startsWith("account.")) {
            var stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
            if (stripeObject instanceof Account account) {
                accountPayload = new AccountPayload(
                        account.getId(),
                        Boolean.TRUE.equals(account.getChargesEnabled()),
                        Boolean.TRUE.equals(account.getPayoutsEnabled()),
                        Boolean.TRUE.equals(account.getDetailsSubmitted()));
            }
        }

        return new ParsedEvent(event.getId(), event.getType(), setupIntentPayload, accountPayload);
    }
}
