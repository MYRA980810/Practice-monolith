package com.livecomerce.auth.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class AliasAlreadyTakenException extends DomainException {

    public AliasAlreadyTakenException(String alias) {
        super("Alias already taken: " + alias);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/alias-already-taken");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
