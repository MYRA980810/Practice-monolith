package com.livecomerce.billing.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class SubscriptionNotFoundException extends DomainException {

    public SubscriptionNotFoundException(String identifier) {
        super("Subscription not found: " + identifier);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/subscription-not-found");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
