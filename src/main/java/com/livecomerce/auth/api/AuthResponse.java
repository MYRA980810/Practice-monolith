package com.livecomerce.auth.api;

import com.livecomerce.auth.application.port.in.AuthResult;

public record AuthResponse(
        String accessToken,
        String tokenType,
        String userId,
        String contact,
        String role,
        String avatarUrl
) {
    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(
                result.accessToken(),
                result.tokenType(),
                result.userId().toString(),
                result.contact(),
                result.role() != null ? result.role().name() : null,
                result.avatarUrl()
        );
    }
}
