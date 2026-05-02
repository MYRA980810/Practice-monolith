package com.livecomerce.catalog.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class ProductNotFoundException extends DomainException {

    public ProductNotFoundException(UUID productId) {
        super("Product not found: " + productId);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/product-not-found");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
