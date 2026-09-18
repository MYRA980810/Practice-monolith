package com.livecomerce.catalog;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Cross-module port exposing catalog price/stock/live-status hydration for
 * the {@code cart} module's storefront cart, batched to avoid one round trip
 * per line. Implemented inside {@code catalog} itself ({@code
 * catalog.application.CartProductInfoAdapter}) — same "root-package
 * interface, implemented by an application-layer adapter in the owning
 * module" shape as {@code com.livecomerce.order.LivePurchasePort}.
 *
 * <p>Fail-closed contract (design D6, confirmed 2026-09-15 — overrides the
 * design's original fail-open recommendation): a line whose price/stock
 * <b>or</b> live-exclusivity signal could not be resolved is <b>absent</b>
 * from the returned map, never defaulted to an available/non-exclusive
 * state. This diverges deliberately from {@code
 * GetProductService#loadLiveStatusSafely}'s fail-open badge default — that
 * pattern is correct for a read-only display badge but wrong for a signal
 * that gates whether a product is purchasable through the general
 * storefront. Callers MUST treat a missing {@link CartLineRef} as
 * "unavailable right now" (same bucket as a genuine out-of-stock line),
 * never as an error.
 */
public interface LoadCartProductInfoPort {

    record CartLineRef(UUID productId, UUID variantId) {}

    record CartProductInfo(
            UUID productId,
            UUID variantId,
            UUID storeId,
            String name,
            String imageUrl,
            BigDecimal unitPrice,
            String currency,
            int availableStock,
            boolean active,
            boolean paused,
            boolean exclusiveToActiveLive
    ) {}

    /**
     * Products/lines absent from the returned map MUST be treated by the
     * caller as unavailable — never assumed to be in-stock or non-exclusive.
     */
    Map<CartLineRef, CartProductInfo> loadForCart(Collection<CartLineRef> refs);
}
