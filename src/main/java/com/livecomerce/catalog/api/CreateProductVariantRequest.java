package com.livecomerce.catalog.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.Map;

public record CreateProductVariantRequest(
        @NotEmpty Map<@NotBlank String, @NotBlank String> options,
        String sku,
        @DecimalMin("0.01") BigDecimal priceOverride
) {}
