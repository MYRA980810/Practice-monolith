package com.livecomerce.payment.application.port.out;

public interface StripeCustomerGatewayPort {

    String createCustomer(String email);
}
