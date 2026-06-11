package com.livecomerce.order.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PlaceOrderItemRequest(
        @NotNull UUID storeId,
        UUID liveSessionId,
        @NotNull UUID productId,
        @NotNull UUID variantId,
        @NotBlank String productName,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
        @NotBlank String currency,
        @Min(1) int quantity
) {}
