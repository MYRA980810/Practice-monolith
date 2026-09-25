package com.livecomerce.store.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class InvalidStoreCategoryException extends DomainException {

    public InvalidStoreCategoryException(UUID categoryId) {
        super("Category " + categoryId + " does not exist or is not active");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/invalid-store-category");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
