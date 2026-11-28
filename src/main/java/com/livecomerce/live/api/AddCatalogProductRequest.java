package com.livecomerce.live.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Name, price and currency are taken from the catalog. Legacy clients that still send
 * {@code nameSnapshot}, {@code priceSnapshot} or {@code currency} keep working: Spring
 * Boot's ObjectMapper does not fail on unknown properties, so those fields are ignored.
 */
public record AddCatalogProductRequest(
        @NotNull UUID productId,
        @NotNull UUID variantId,
        @Min(1) int stockAllocated,
        String imageUrl
) {}
