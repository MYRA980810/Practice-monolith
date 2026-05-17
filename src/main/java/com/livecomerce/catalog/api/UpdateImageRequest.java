package com.livecomerce.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record UpdateImageRequest(
        @NotBlank @Size(max = 500) String url,
        @NotNull int position,
        boolean primary
) {}
