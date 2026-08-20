package com.livecomerce.auth.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class ChangePasswordTokenInvalidException extends DomainException {

    public ChangePasswordTokenInvalidException() {
        super("Change password token is invalid or expired.");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/change-password-token-invalid");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
