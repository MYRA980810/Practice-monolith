package com.livecomerce.cart.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * {@code quantity} is capped at 99 to mirror {@code
 * com.livecomerce.cart.domain.CartItem#MAX_QUANTITY} (the domain's source of
 * truth for the cap). The api layer duplicates the literal rather than
 * depending on the domain class directly, following this codebase's existing
 * convention (see {@code catalog.api.AddStockRequest}/{@code
 * CorrectStockRequest}, which bound quantities without referencing domain
 * constants either) — keep both in sync if the cap ever changes. No single
 * add-to-cart call legitimately needs to request more than the entire cap at
 * once.
 */
record AddToCartRequest(
        @NotNull UUID productId,
        UUID variantId,
        @Min(1) @Max(99) int quantity
) {}
