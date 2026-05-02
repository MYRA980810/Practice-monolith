package com.livecomerce.billing.application;

import com.livecomerce.billing.application.port.in.CreateCheckoutUseCase;
import com.livecomerce.billing.application.port.out.PaymentGatewayPort;
import com.livecomerce.billing.infrastructure.config.BillingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateCheckoutService implements CreateCheckoutUseCase {

    private final List<PaymentGatewayPort> gateways;
    private final BillingProperties billingProperties;

    @Override
    public CheckoutResult createCheckout(CreateCheckoutCommand command) {
        var gateway = gateways.stream()
                .filter(g -> g.gateway() == command.gateway())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Gateway no soportado: " + command.gateway()));

        var session = gateway.createCheckout(
                command.userId(),
                command.plan(),
                billingProperties.successUrl(),
                billingProperties.cancelUrl()
        );

        return new CheckoutResult(session.checkoutUrl());
    }
}
