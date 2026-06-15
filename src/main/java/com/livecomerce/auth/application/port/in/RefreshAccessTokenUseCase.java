package com.livecomerce.auth.application.port.in;

public interface RefreshAccessTokenUseCase {

    /**
     * Validates the given raw refresh token, rotates it, and returns a new access token
     * plus a new raw refresh token.
     */
    RefreshResult refresh(String rawRefreshToken);

    record RefreshResult(String accessToken, String refreshToken) {}
}
