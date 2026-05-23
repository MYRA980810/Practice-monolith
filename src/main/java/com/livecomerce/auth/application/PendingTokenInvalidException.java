package com.livecomerce.auth.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class PendingTokenInvalidException extends DomainException {

    public PendingTokenInvalidException() {
        super("Pending verification token is invalid or expired.");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/pending-token-invalid");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
