package com.livecomerce.auth.api;

import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(
        @NotBlank String pendingToken,
        @NotBlank String code
) {}
