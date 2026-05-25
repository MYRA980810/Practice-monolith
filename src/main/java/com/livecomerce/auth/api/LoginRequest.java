package com.livecomerce.auth.api;

import com.livecomerce.auth.api.validation.ValidContact;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @ValidContact @NotBlank String contact,
        @NotBlank String password
) {}
