package com.livecomerce.review.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class ReviewOrderNotFoundException extends DomainException {

    public ReviewOrderNotFoundException(UUID orderId) {
        super("Order not found: " + orderId);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/review-order-not-found");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
