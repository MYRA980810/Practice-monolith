package com.livecomerce.media.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class InvalidImageException extends DomainException {

    public InvalidImageException(String reason) {
        super("Invalid image: " + reason);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/invalid-image");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
