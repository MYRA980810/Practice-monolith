package com.livecomerce.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateAvatarRequest(
        @NotBlank @URL @Size(max = 512) String avatarUrl
) {}
