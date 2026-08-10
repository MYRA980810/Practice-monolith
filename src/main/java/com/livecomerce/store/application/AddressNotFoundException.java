package com.livecomerce.store.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class AddressNotFoundException extends DomainException {

    public AddressNotFoundException(UUID addressId) {
        super("Address not found: " + addressId);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/address-not-found");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
