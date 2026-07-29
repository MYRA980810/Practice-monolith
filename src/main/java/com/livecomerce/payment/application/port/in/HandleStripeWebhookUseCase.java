package com.livecomerce.payment.application.port.in;

public interface HandleStripeWebhookUseCase {

    record WebhookCommand(String payload, String signature) {}

    void handle(WebhookCommand cmd);
}
