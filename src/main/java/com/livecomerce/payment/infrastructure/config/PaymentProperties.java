package com.livecomerce.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment")
public record PaymentProperties(
        StripeProperties stripe
) {
    public record StripeProperties(
            String apiKey,
            String webhookSecret,
            ConnectProperties connect
    ) {}

    public record ConnectProperties(
            String refreshUrl,
            String returnUrl
    ) {}
}
