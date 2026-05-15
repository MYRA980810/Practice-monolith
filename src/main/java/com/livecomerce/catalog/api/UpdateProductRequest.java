package com.livecomerce.catalog.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

record UpdateProductRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotNull @DecimalMin("0.01") BigDecimal basePrice,
        @NotBlank @Size(max = 3) String currency,
        @Size(max = 100) String sku
) {}
