package com.livecomerce.cart.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record AddToCartRequest(
        @NotNull UUID productId,
        UUID variantId,
        @Min(1) int quantity
) {}
