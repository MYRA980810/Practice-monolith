package com.livecomerce.auth.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class ContactNotFoundException extends DomainException {

    public ContactNotFoundException() {
        super("Contact not found.");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/contact-not-found");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
