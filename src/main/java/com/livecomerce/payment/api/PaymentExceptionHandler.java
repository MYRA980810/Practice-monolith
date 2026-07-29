package com.livecomerce.payment.api;

import com.livecomerce.payment.application.InvalidWebhookSignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@SuppressWarnings("null") // URI.create() is never null; ProblemDetail.setType expects @NonNull
@RestControllerAdvice(basePackages = "com.livecomerce.payment")
class PaymentExceptionHandler {

    @ExceptionHandler(InvalidWebhookSignatureException.class)
    ProblemDetail handleInvalidWebhookSignature(InvalidWebhookSignatureException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/invalid-webhook-signature"));
        return detail;
    }
}
