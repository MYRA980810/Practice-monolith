package com.livecomerce.store.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class SlugAlreadyTakenException extends DomainException {

    public SlugAlreadyTakenException(String slug) {
        super("Slug already taken: " + slug);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/slug-already-taken");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
