package com.livecomerce.store.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class StoreAlreadyExistsException extends DomainException {

    public StoreAlreadyExistsException(UUID userId) {
        super("User already has a store: " + userId);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/store-already-exists");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
