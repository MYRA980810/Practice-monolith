package com.livecomerce.live.api;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddHotProductRequest(
        @NotBlank String name,
        @NotNull BigDecimal price,
        @NotBlank String currency,
        @Min(1) int stockAllocated,
        @Nullable String imageUrl
) {}
