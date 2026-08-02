package com.livecomerce.auth.application.port.in;

import com.livecomerce.auth.domain.Role;

import java.util.UUID;

public record UserProfileResult(
        UUID id,
        String email,
        String phone,
        Role role,
        String firstName,
        String lastName,
        String alias,
        String avatarUrl,
        boolean profileComplete
) {
}
