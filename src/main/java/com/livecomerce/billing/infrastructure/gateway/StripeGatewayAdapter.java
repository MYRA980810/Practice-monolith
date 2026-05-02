package com.livecomerce.billing.infrastructure.gateway;

import com.livecomerce.billing.application.PaymentGatewayException;
import com.livecomerce.billing.application.port.out.PaymentGatewayPort;
import com.livecomerce.billing.domain.Gateway;
import com.livecomerce.billing.infrastructure.config.BillingProperties;
import com.livecomerce.shared.Plan;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class StripeGatewayAdapter implements PaymentGatewayPort {

    private final BillingProperties.StripeProperties stripeConfig;

    StripeGatewayAdapter(BillingProperties billingProperties) {
        this.stripeConfig = billingProperties.stripe();
        Stripe.apiKey = stripeConfig.apiKey();
    }

    @Override
    public Gateway gateway() {
        return Gateway.STRIPE;
    }

    @Override
    public CheckoutSession createCheckout(UUID userId, Plan plan, String successUrl, String cancelUrl) {
        var priceId = stripeConfig.priceIds().get(plan.getCode());
        if (priceId == null) {
            throw new IllegalArgumentException("No hay precio de Stripe configurado para el plan: " + plan.getCode());
        }

        try {
            var params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .putMetadata("userId", userId.toString())
                    .putMetadata("plan", plan.getCode())
                    .build();

            var session = Session.create(params);

            return new CheckoutSession(session.getUrl(), session.getId());

        } catch (StripeException e) {
            throw new PaymentGatewayException("Error al crear sesión de Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public WebhookEvent parseWebhook(String payload, String signature) {
        throw new UnsupportedOperationException("Stripe webhook pendiente");
    }
}
