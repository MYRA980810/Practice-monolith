package com.livecomerce.catalog.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class ProductCannotBePausedException extends DomainException {

    public ProductCannotBePausedException(UUID productId) {
        super("Product %s cannot be paused because it is inactive".formatted(productId));
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/product-cannot-be-paused");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
