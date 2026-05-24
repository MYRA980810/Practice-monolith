package com.livecomerce.auth.application;

public class OAuthPendingTokenInvalidException extends RuntimeException {
    public OAuthPendingTokenInvalidException() {
        super("OAuth pending token is invalid or expired");
    }
}
