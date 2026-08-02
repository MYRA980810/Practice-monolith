package com.livecomerce.payment.infrastructure.stripe;

import com.livecomerce.payment.application.PaymentGatewayException;
import com.livecomerce.payment.application.port.out.StripeCustomerGatewayPort;
import com.livecomerce.payment.infrastructure.config.PaymentProperties;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import org.springframework.stereotype.Component;

@Component
class StripeCustomerGatewayAdapter implements StripeCustomerGatewayPort {

    private final PaymentProperties paymentProperties;

    StripeCustomerGatewayAdapter(PaymentProperties paymentProperties) {
        this.paymentProperties = paymentProperties;
        // com.stripe.Stripe.apiKey is a single static field shared across the whole JVM, so only
        // one of the four new Stripe adapters in this module needs to set it here at construction
        // time (billing.StripeGatewayAdapter does the same for its own key). Setting it more than
        // once is harmless but redundant, so we keep it to this single adapter.
        Stripe.apiKey = paymentProperties.stripe().apiKey();
    }

    @Override
    public String createCustomer(String email) {
        try {
            var paramsBuilder = CustomerCreateParams.builder();

            if (email != null) {
                paramsBuilder.setEmail(email);
            }

            var params = paramsBuilder.build();

            var customer = Customer.create(params);

            return customer.getId();

        } catch (StripeException e) {
            throw new PaymentGatewayException("Error al crear customer de Stripe: " + e.getMessage(), e);
        }
    }
}
