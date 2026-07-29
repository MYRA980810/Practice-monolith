package com.livecomerce.payment.application.port.out;

public interface ProcessedWebhookEventPort {

    boolean tryMarkProcessed(String stripeEventId, String type);
}
