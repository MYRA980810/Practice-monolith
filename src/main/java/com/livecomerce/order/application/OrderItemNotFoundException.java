package com.livecomerce.order.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class OrderItemNotFoundException extends DomainException {

    public OrderItemNotFoundException(UUID itemId) {
        super("Order item not found: " + itemId);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/order-item-not-found");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
