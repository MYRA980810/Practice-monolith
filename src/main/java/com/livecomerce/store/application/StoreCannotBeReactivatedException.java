package com.livecomerce.store.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class StoreCannotBeReactivatedException extends DomainException {

    public StoreCannotBeReactivatedException() {
        super("Store is suspended, resolve the issue before reactivating");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/store-cannot-be-reactivated");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
