package com.livecomerce.store.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class StoreCannotBeReopenedException extends DomainException {

    public StoreCannotBeReopenedException() {
        super("Store is suspended, resolve the issue before reopening");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/store-cannot-be-reopened");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
