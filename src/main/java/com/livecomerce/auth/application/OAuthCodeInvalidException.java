package com.livecomerce.auth.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class OAuthCodeInvalidException extends DomainException {

    public OAuthCodeInvalidException() {
        super("OAuth2 exchange code is invalid, expired, or already used");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/oauth-code-invalid");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
