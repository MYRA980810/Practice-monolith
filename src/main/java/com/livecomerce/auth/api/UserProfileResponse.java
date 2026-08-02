package com.livecomerce.auth.api;

import com.livecomerce.auth.application.port.in.UserProfileResult;

public record UserProfileResponse(
        String id,
        String email,
        String phone,
        String role,
        String firstName,
        String lastName,
        String alias,
        String avatarUrl,
        boolean profileComplete
) {
    public static UserProfileResponse from(UserProfileResult result) {
        return new UserProfileResponse(
                result.id().toString(),
                result.email(),
                result.phone(),
                result.role() != null ? result.role().name() : null,
                result.firstName(),
                result.lastName(),
                result.alias(),
                result.avatarUrl(),
                result.profileComplete()
        );
    }
}
