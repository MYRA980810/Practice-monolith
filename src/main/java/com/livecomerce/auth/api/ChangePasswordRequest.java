package com.livecomerce.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String changePasswordToken,
        @NotBlank @Size(min = 8, max = 100) String newPassword,
        @NotBlank @Size(min = 8, max = 100) String confirmPassword
) {}
