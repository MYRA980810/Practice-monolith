package com.livecomerce.auth.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class PhoneAlreadyTakenException extends DomainException {

    public PhoneAlreadyTakenException(String phone) {
        super("Phone number already taken: " + phone);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/phone-already-taken");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
