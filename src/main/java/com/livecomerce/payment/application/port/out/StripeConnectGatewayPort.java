package com.livecomerce.payment.application.port.out;

public interface StripeConnectGatewayPort {

    String createExpressAccount(String email);

    String createAccountLink(String stripeAccountId, String refreshUrl, String returnUrl);
}
