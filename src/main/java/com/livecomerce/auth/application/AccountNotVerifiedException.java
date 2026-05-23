package com.livecomerce.auth.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class AccountNotVerifiedException extends DomainException {

    public AccountNotVerifiedException() {
        super("Account is not verified. Please complete OTP verification.");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/account-not-verified");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
