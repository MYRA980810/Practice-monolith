package com.livecomerce.review.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class ReviewAlreadyExistsException extends DomainException {

    public ReviewAlreadyExistsException(UUID orderId) {
        super("Order %s already has a review".formatted(orderId));
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/review-already-exists");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
