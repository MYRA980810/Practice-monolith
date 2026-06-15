package com.livecomerce.auth.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class RefreshTokenInvalidException extends DomainException {

    public RefreshTokenInvalidException() {
        super("Refresh token is invalid or expired");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/refresh-invalid");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
