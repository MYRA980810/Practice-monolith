package com.livecomerce.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

record CreateProductRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotNull @DecimalMin("0.01") BigDecimal basePrice,
        @Size(max = 3) String currency,
        @Size(max = 100) String sku,
        @NotNull UUID categoryId,
        @Valid List<ProductImageRequest> images
) {}
