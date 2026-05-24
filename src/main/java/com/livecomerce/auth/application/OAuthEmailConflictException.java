package com.livecomerce.auth.application;

public class OAuthEmailConflictException extends RuntimeException {
    public OAuthEmailConflictException() {
        super("Email already registered with a different authentication method");
    }
}
