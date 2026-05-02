package com.livecomerce.billing.infrastructure.gateway;

import com.livecomerce.billing.application.port.out.PaymentGatewayPort;
import com.livecomerce.billing.domain.Gateway;
import com.livecomerce.shared.Plan;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class ConektaGatewayAdapter implements PaymentGatewayPort {

    @Override
    public Gateway gateway() {
        return Gateway.CONEKTA;
    }

    @Override
    public CheckoutSession createCheckout(UUID storeId, Plan plan, String successUrl, String cancelUrl) {
        // TODO: integrar SDK de Conekta
        // Incluir storeId + plan como metadata para recuperarlo en el webhook
        throw new UnsupportedOperationException("Conekta integration pending");
    }

    @Override
    public WebhookEvent parseWebhook(String payload, String signature) {
        // TODO: verificar firma Digest y parsear notificación de Conekta
        throw new UnsupportedOperationException("Conekta integration pending");
    }
}
