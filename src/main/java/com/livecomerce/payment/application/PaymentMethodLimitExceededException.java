package com.livecomerce.payment.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class PaymentMethodLimitExceededException extends DomainException {

    public PaymentMethodLimitExceededException(UUID buyerId) {
        super("User " + buyerId + " already has the maximum of 6 payment methods");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/payment-method-limit-exceeded");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
