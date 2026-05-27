package com.livecomerce.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String resetToken,
        @NotBlank @Size(min = 8, max = 100) String newPassword,
        @NotBlank @Size(min = 8, max = 100) String confirmPassword
) {}
