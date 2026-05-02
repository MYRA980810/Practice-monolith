package com.livecomerce.auth.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class EmailAlreadyTakenException extends DomainException {

    public EmailAlreadyTakenException(String email) {
        super("Email already taken: " + email);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/email-already-taken");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
