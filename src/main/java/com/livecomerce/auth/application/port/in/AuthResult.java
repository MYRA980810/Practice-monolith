package com.livecomerce.auth.application.port.in;

import com.livecomerce.auth.domain.Role;

import java.util.UUID;

public record AuthResult(
        String accessToken,
        String tokenType,
        UUID userId,
        String contact,
        Role role,
        String avatarUrl,
        String refreshToken,
        boolean profileComplete
) {

    public static AuthResult of(
            String token,
            UUID userId,
            String contact,
            Role role,
            String avatarUrl,
            String refreshToken,
            boolean profileComplete
    ) {
        return new AuthResult(token, "Bearer", userId, contact, role, avatarUrl, refreshToken, profileComplete);
    }
}
