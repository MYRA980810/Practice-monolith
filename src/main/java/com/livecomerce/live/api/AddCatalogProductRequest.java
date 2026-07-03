package com.livecomerce.live.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AddCatalogProductRequest(
        @NotNull UUID productId,
        @NotNull UUID variantId,
        @NotBlank String nameSnapshot,
        @NotNull BigDecimal priceSnapshot,
        @NotBlank String currency,
        @Min(1) int stockAllocated,
        String imageUrl
) {}
