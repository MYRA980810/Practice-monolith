package com.livecomerce.live;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Reads a catalog product variant so a live can snapshot it from the source of truth
 * instead of trusting client-supplied name/price. Implemented by the {@code catalog}
 * module (which already depends on {@code live}) so {@code live} never depends on it.
 */
public interface LoadCatalogProductPort {

    /**
     * @param price    the variant's price override if it defines one, otherwise the product's base price
     * @param imageUrl the product's primary image (or first image), {@code null} if it has none
     */
    record CatalogProductSnapshot(
            UUID productId,
            UUID variantId,
            UUID storeId,
            String name,
            BigDecimal price,
            String currency,
            String imageUrl,
            boolean active,
            boolean paused
    ) {
        /** Buyer-visible in the catalog: not deactivated and not paused. */
        public boolean sellable() {
            return active && !paused;
        }
    }

    /** Why a lookup found nothing; for server-side diagnostics only, never exposed to clients. */
    enum LookupStatus { FOUND, PRODUCT_NOT_FOUND, VARIANT_NOT_IN_PRODUCT }

    /** @param snapshot non-null only when {@code status} is {@link LookupStatus#FOUND} */
    record CatalogProductLookup(LookupStatus status, CatalogProductSnapshot snapshot) {

        public static CatalogProductLookup found(CatalogProductSnapshot snapshot) {
            return new CatalogProductLookup(LookupStatus.FOUND, Objects.requireNonNull(snapshot));
        }

        public static CatalogProductLookup productNotFound() {
            return new CatalogProductLookup(LookupStatus.PRODUCT_NOT_FOUND, null);
        }

        public static CatalogProductLookup variantNotInProduct() {
            return new CatalogProductLookup(LookupStatus.VARIANT_NOT_IN_PRODUCT, null);
        }
    }

    CatalogProductLookup loadForLive(UUID productId, UUID variantId);
}
