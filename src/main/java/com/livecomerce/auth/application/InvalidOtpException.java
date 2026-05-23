package com.livecomerce.auth.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class InvalidOtpException extends DomainException {

    public InvalidOtpException() {
        super("Invalid or expired verification code.");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/invalid-otp");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
