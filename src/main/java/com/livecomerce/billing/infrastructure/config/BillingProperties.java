package com.livecomerce.billing.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app.billing")
public record BillingProperties(
        String successUrl,
        String cancelUrl,
        StripeProperties stripe
) {
    public record StripeProperties(
            String apiKey,
            String webhookSecret,
            Map<String, String> priceIds
    ) {}
}
