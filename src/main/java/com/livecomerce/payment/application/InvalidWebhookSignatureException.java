package com.livecomerce.payment.application;

public class InvalidWebhookSignatureException extends RuntimeException {

    public InvalidWebhookSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
