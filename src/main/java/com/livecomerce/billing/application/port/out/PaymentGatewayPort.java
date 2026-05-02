package com.livecomerce.billing.application.port.out;

import com.livecomerce.billing.domain.Gateway;
import com.livecomerce.shared.Plan;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface PaymentGatewayPort {

    record CheckoutSession(String checkoutUrl, String externalId) {}

    record WebhookEvent(
            UUID userId,
            Plan plan,
            String externalId,
            OffsetDateTime periodEnd,
            WebhookEventType type
    ) {}

    enum WebhookEventType {
        PAYMENT_SUCCEEDED,
        SUBSCRIPTION_RENEWED,
        SUBSCRIPTION_CANCELLED
    }

    Gateway gateway();

    CheckoutSession createCheckout(UUID userId, Plan plan, String successUrl, String cancelUrl);

    WebhookEvent parseWebhook(String payload, String signature);
}
