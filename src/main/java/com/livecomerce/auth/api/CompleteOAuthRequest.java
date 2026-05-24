package com.livecomerce.auth.api;

import com.livecomerce.auth.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompleteOAuthRequest(
        @NotBlank String pendingToken,
        @NotNull Role role
) {}
