package com.livecomerce.store.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateStoreRequest(

        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 2000)
        String description,

        @URL
        @Size(max = 500)
        String logoUrl
) {}
