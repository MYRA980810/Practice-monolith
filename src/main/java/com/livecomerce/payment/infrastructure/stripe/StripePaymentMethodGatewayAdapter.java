package com.livecomerce.payment.infrastructure.stripe;

import com.livecomerce.payment.application.PaymentGatewayException;
import com.livecomerce.payment.application.port.out.StripePaymentMethodGatewayPort;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SetupIntent;
import com.stripe.param.SetupIntentCreateParams;
import org.springframework.stereotype.Component;

@Component
class StripePaymentMethodGatewayAdapter implements StripePaymentMethodGatewayPort {

    @Override
    public SetupIntentHandle createSetupIntent(String stripeCustomerId) {
        try {
            var params = SetupIntentCreateParams.builder()
                    .setCustomer(stripeCustomerId)
                    .setAutomaticPaymentMethods(
                            SetupIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            var setupIntent = SetupIntent.create(params);

            return new SetupIntentHandle(setupIntent.getClientSecret(), setupIntent.getId());

        } catch (StripeException e) {
            throw new PaymentGatewayException("Error al crear SetupIntent de Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentMethodDetails retrievePaymentMethod(String stripePaymentMethodId) {
        try {
            var paymentMethod = PaymentMethod.retrieve(stripePaymentMethodId);
            var card = paymentMethod.getCard();

            // Stripe SDK 26.3.0 returns Card.expMonth/expYear as Long, not Integer.
            return new PaymentMethodDetails(
                    card.getBrand(),
                    card.getLast4(),
                    card.getExpMonth().intValue(),
                    card.getExpYear().intValue()
            );

        } catch (StripeException e) {
            throw new PaymentGatewayException("Error al obtener payment method de Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public void detachPaymentMethod(String stripePaymentMethodId) {
        try {
            PaymentMethod.retrieve(stripePaymentMethodId).detach();

        } catch (StripeException e) {
            throw new PaymentGatewayException("Error al desvincular payment method de Stripe: " + e.getMessage(), e);
        }
    }
}
