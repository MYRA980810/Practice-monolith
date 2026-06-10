package com.livecomerce.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

record CreateCategoryRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "[a-z0-9\\-]+") String slug
) {}
