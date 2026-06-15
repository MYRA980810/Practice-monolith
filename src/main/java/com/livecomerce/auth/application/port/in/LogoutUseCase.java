package com.livecomerce.auth.application.port.in;

public interface LogoutUseCase {

    /**
     * Revokes the refresh token family associated with the given raw token.
     * Safe to call with null (e.g. when no cookie is present).
     */
    void logout(String rawRefreshToken);
}
