package com.livecomerce.auth.application.port.in;

import com.livecomerce.auth.domain.Role;

import java.util.UUID;

public record AuthResult(String accessToken, String tokenType, UUID userId, String email, Role role) {

    public static AuthResult of(String token, UUID userId, String email, Role role) {
        return new AuthResult(token, "Bearer", userId, email, role);
    }
}
