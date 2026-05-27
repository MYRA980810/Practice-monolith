package com.livecomerce.auth.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class PasswordMismatchException extends DomainException {

    public PasswordMismatchException() {
        super("Passwords do not match.");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/password-mismatch");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
