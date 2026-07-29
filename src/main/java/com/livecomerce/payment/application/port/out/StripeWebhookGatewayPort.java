package com.livecomerce.payment.application.port.out;

public interface StripeWebhookGatewayPort {

    ParsedEvent verifyAndParse(String payload, String signature);

    record ParsedEvent(
            String eventId,
            String eventType,
            SetupIntentPayload setupIntent,   // null si el evento no es de setup_intent
            AccountPayload account            // null si el evento no es de account
    ) {}

    record SetupIntentPayload(String stripeCustomerId, String stripePaymentMethodId) {}

    record AccountPayload(String stripeAccountId, boolean chargesEnabled, boolean payoutsEnabled,
            boolean detailsSubmitted) {}
}
