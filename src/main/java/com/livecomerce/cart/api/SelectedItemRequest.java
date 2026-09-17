package com.livecomerce.cart.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record SelectedItemRequest(
        @NotNull UUID storeId,
        @NotNull UUID productId,
        UUID variantId
) {}
