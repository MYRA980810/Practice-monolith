package com.livecomerce.review.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class ReviewOrderNotOwnedException extends DomainException {

    public ReviewOrderNotOwnedException(UUID orderId, UUID buyerId) {
        super("Order %s does not belong to buyer %s".formatted(orderId, buyerId));
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/review-order-not-owned");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
