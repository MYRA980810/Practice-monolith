package com.livecomerce.catalog.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class ProductImageNotFoundException extends DomainException {

    public ProductImageNotFoundException(UUID imageId) {
        super("Product image not found: " + imageId);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/product-image-not-found");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
