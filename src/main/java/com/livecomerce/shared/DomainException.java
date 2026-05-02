package com.livecomerce.shared;

import org.springframework.http.HttpStatus;

import java.net.URI;

public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    public abstract URI getType();

    public abstract HttpStatus getStatus();
}
