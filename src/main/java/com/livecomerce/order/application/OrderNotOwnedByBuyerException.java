package com.livecomerce.order.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class OrderNotOwnedByBuyerException extends DomainException {

    public OrderNotOwnedByBuyerException(UUID orderId, UUID buyerId) {
        super("Order %s does not belong to buyer %s".formatted(orderId, buyerId));
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/order-not-owned-by-buyer");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
