package com.livecomerce.store.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class AddressLimitExceededException extends DomainException {

    public AddressLimitExceededException(String kind, UUID userId) {
        super("User " + userId + " already has the maximum of 6 " + kind + " addresses");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/address-limit-exceeded");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
