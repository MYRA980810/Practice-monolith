package com.livecomerce.live.domain;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class ProfileIncompleteException extends DomainException {

    public ProfileIncompleteException(UUID userId) {
        super("User " + userId + " must complete their profile (address + payment method) before performing this action");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/profile-incomplete");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
