package com.livecomerce.auth.api;

import jakarta.validation.constraints.NotBlank;

public record VerifyResetRequest(@NotBlank String pendingToken, @NotBlank String code) {}
