package com.livecomerce.catalog.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record AddImageRequest(
        @NotBlank @Size(max = 500) String url,
        @Min(0) int position,
        boolean primary
) {}
