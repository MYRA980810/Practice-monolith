package com.livecomerce.catalog.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class CategorySlugAlreadyTakenException extends DomainException {

    public CategorySlugAlreadyTakenException(String slug) {
        super("Category slug already taken: " + slug);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/category-slug-already-taken");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
