package com.livecomerce.live.domain;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class CategoryNotAvailableException extends DomainException {

    public CategoryNotAvailableException(UUID categoryId) {
        super("Category " + categoryId + " does not exist or is not active");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/category-not-available");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
