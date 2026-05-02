package com.livecomerce.store.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class StoreNotFoundException extends DomainException {

    public StoreNotFoundException(String identifier) {
        super("Store not found: " + identifier);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/store-not-found");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
