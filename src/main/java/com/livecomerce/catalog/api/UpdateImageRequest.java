package com.livecomerce.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record UpdateImageRequest(
        @NotBlank @Size(max = 500) String url,
        Integer position,
        Boolean primary
) {}
