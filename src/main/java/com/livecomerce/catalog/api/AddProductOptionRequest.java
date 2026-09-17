package com.livecomerce.catalog.api;

import com.livecomerce.catalog.domain.OptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddProductOptionRequest(
        @NotBlank String name,
        @NotNull OptionType type,
        @NotEmpty List<@NotBlank String> values
) {}
