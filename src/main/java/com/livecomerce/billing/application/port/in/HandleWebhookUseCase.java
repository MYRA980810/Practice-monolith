package com.livecomerce.billing.application.port.in;

import com.livecomerce.billing.domain.Gateway;

public interface HandleWebhookUseCase {

    record WebhookCommand(
            Gateway gateway,
            String payload,
            String signature
    ) {}

    void handle(WebhookCommand command);
}
