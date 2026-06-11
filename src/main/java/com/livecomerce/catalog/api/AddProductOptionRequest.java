package com.livecomerce.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddProductOptionRequest(
        @NotBlank String name,
        @NotEmpty List<@NotBlank String> values
) {}
