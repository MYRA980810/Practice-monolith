package com.livecomerce.store.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CreateStoreRequest(

        @NotBlank
        @Size(max = 255)
        String name,

        @NotBlank
        @Size(max = 255)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                 message = "must be lowercase letters, numbers, and hyphens only")
        String slug,

        @Size(max = 2000)
        String description,

        @URL
        @Size(max = 500)
        String logoUrl
) {}
