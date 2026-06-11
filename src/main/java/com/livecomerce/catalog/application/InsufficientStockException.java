package com.livecomerce.catalog.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class InsufficientStockException extends DomainException {

    public InsufficientStockException(UUID variantId, int requested, int available) {
        super("Insufficient stock for variant %s: requested=%d, available=%d"
                .formatted(variantId, requested, available));
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/insufficient-stock");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
