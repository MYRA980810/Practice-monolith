package com.livecomerce.cart.domain;

import java.util.UUID;

/**
 * Identifies a single cart line: a product, optionally scoped to a specific
 * variant. Two lines are the same line — and therefore must be merged
 * instead of duplicated — if and only if both {@code productId} and {@code
 * variantId} match. A {@code null} variantId is itself a valid, distinct
 * identity (products without a variant selection).
 */
public record CartLineKey(UUID productId, UUID variantId) {

    public CartLineKey {
        if (productId == null) {
            throw new IllegalArgumentException("productId must not be null");
        }
    }
}
