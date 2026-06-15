package com.livecomerce.auth.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class RefreshTokenReuseException extends DomainException {

    public RefreshTokenReuseException() {
        super("Refresh token reuse detected — the token family has been revoked");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/refresh-reuse");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
