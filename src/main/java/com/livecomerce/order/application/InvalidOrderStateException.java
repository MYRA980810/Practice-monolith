package com.livecomerce.order.application;

import com.livecomerce.shared.DomainException;
import com.livecomerce.order.domain.OrderStatus;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class InvalidOrderStateException extends DomainException {

    public InvalidOrderStateException(UUID orderId, OrderStatus current, String operation) {
        super("Cannot perform '%s' on order %s with status %s".formatted(operation, orderId, current));
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/invalid-order-state");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
