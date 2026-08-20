package com.livecomerce.auth.api;

import jakarta.validation.constraints.NotBlank;

public record VerifyCurrentPasswordRequest(@NotBlank String currentPassword) {}
