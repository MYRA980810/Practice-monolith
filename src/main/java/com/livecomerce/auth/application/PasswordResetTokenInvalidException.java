package com.livecomerce.auth.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class PasswordResetTokenInvalidException extends DomainException {

    public PasswordResetTokenInvalidException() {
        super("Password reset token is invalid or expired.");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/password-reset-token-invalid");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
