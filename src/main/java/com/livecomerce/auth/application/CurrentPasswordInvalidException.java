package com.livecomerce.auth.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class CurrentPasswordInvalidException extends DomainException {

    public CurrentPasswordInvalidException() {
        super("Current password is invalid.");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/current-password-invalid");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
