package com.livecomerce.catalog.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class ProductVariantNotFoundException extends DomainException {

    public ProductVariantNotFoundException(UUID variantId) {
        super("Product variant not found: " + variantId);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/product-variant-not-found");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
