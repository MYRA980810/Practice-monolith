package com.livecomerce.payment.application.port.out;

public interface StripePaymentMethodGatewayPort {

    record SetupIntentHandle(String clientSecret, String setupIntentId) {}

    record PaymentMethodDetails(String brand, String last4, int expMonth, int expYear) {}

    SetupIntentHandle createSetupIntent(String stripeCustomerId);

    PaymentMethodDetails retrievePaymentMethod(String stripePaymentMethodId);

    void detachPaymentMethod(String stripePaymentMethodId);
}
