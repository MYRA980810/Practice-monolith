package com.livecomerce.review.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class OrderNotEligibleForReviewException extends DomainException {

    public OrderNotEligibleForReviewException(UUID orderId) {
        super("Order %s is not eligible for review — it must be delivered first".formatted(orderId));
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/order-not-eligible-for-review");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
