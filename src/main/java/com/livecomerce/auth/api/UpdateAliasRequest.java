package com.livecomerce.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAliasRequest(
        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                 message = "must be lowercase letters, numbers, and hyphens only")
        String alias
) {}
