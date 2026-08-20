package com.livecomerce.auth.api;

import jakarta.validation.constraints.NotBlank;

public record VerifyChangePasswordOtpRequest(@NotBlank String pendingToken, @NotBlank String code) {}
