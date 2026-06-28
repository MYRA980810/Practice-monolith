package com.livecomerce.live.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class LiveProductTest {

    private static final UUID    SELLER_ID  = UUID.randomUUID();
    private static final UUID    STORE_ID   = UUID.randomUUID();
    private static final UUID    PRODUCT_ID = UUID.randomUUID();
    private static final UUID    VARIANT_ID = UUID.randomUUID();
    private static final BigDecimal PRICE   = new BigDecimal("99.90");

    private Live buildLive() {
        return Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Test Live", null, null, 60);
    }

    // ── forCatalogProduct ─────────────────────────────────────────────────────

    @Test
    void forCatalogProduct_createsWithCorrectFields() {
        var live = buildLive();
        var lp = LiveProduct.forCatalogProduct(live, PRODUCT_ID, VARIANT_ID, "Shirt", PRICE, "MXN", 50);

        assertThat(lp.getId()).isNotNull();
        assertThat(lp.getLive()).isEqualTo(live);
        assertThat(lp.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(lp.getVariantId()).isEqualTo(VARIANT_ID);
        assertThat(lp.getProductNameSnapshot()).isEqualTo("Shirt");
        assertThat(lp.getPriceSnapshot()).isEqualByComparingTo(PRICE);
        assertThat(lp.getCurrencySnapshot()).isEqualTo("MXN");
        assertThat(lp.getStockAllocated()).isEqualTo(50);
        assertThat(lp.isHot()).isFalse();
        assertThat(lp.isPinned()).isFalse();
        assertThat(lp.getStockSold()).isZero();
    }

    // ── forHotProduct ─────────────────────────────────────────────────────────

    @Test
    void forHotProduct_createsWithNullProductAndVariant() {
        var live = buildLive();
        var lp = LiveProduct.forHotProduct(live, "Mystery Box", PRICE, "MXN", 10, null);

        assertThat(lp.isHot()).isTrue();
        assertThat(lp.getProductId()).isNull();
        assertThat(lp.getVariantId()).isNull();
        assertThat(lp.getProductNameSnapshot()).isEqualTo("Mystery Box");
        assertThat(lp.getStockAllocated()).isEqualTo(10);
    }

    @Test
    void forHotProduct_zeroStock_throwsIllegalArgumentException() {
        var live = buildLive();

        assertThatThrownBy(() ->
                LiveProduct.forHotProduct(live, "Mystery Box", PRICE, "MXN", 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void forHotProduct_negativeStock_throwsIllegalArgumentException() {
        var live = buildLive();

        assertThatThrownBy(() ->
                LiveProduct.forHotProduct(live, "Mystery Box", PRICE, "MXN", -1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── pin / unpin ───────────────────────────────────────────────────────────

    @Test
    void pin_setsIsPinnedTrue() {
        var live = buildLive();
        var lp = LiveProduct.forCatalogProduct(live, PRODUCT_ID, VARIANT_ID, "Shirt", PRICE, "MXN", 50);

        lp.pin();

        assertThat(lp.isPinned()).isTrue();
    }

    @Test
    void unpin_setsIsPinnedFalse() {
        var live = buildLive();
        var lp = LiveProduct.forCatalogProduct(live, PRODUCT_ID, VARIANT_ID, "Shirt", PRICE, "MXN", 50);
        lp.pin();

        lp.unpin();

        assertThat(lp.isPinned()).isFalse();
    }

    // ── tryAtomicStockReserve ─────────────────────────────────────────────────

    @Test
    void tryAtomicStockReserve_withAvailableStock_doesNotThrow() {
        var live = buildLive();
        var lp = LiveProduct.forCatalogProduct(live, PRODUCT_ID, VARIANT_ID, "Shirt", PRICE, "MXN", 10);

        assertThatNoException().isThrownBy(lp::tryAtomicStockReserve);
    }

    @Test
    void tryAtomicStockReserve_whenStockSoldEqualsAllocated_throwsOutOfStock() {
        var live = buildLive();
        var lp = LiveProduct.forCatalogProduct(live, PRODUCT_ID, VARIANT_ID, "Shirt", PRICE, "MXN", 1);
        lp.incrementStockSold(); // sell the only unit

        assertThatThrownBy(lp::tryAtomicStockReserve)
                .isInstanceOf(LiveProductOutOfStockException.class);
    }

    @Test
    void tryAtomicStockReserve_hotProduct_neverThrowsRegardlessOfStock() {
        var live = buildLive();
        // Hot product has stock allocated, but domain check skips for hot
        var lp = LiveProduct.forHotProduct(live, "Mystery Box", PRICE, "MXN", 1, null);
        lp.incrementStockSold(); // sold 1
        lp.incrementStockSold(); // sold 2 — beyond allocated

        // hot products bypass stock check at domain level
        assertThatNoException().isThrownBy(lp::tryAtomicStockReserve);
    }

    // ── incrementStockSold ────────────────────────────────────────────────────

    @Test
    void incrementStockSold_increasesCounter() {
        var live = buildLive();
        var lp = LiveProduct.forCatalogProduct(live, PRODUCT_ID, VARIANT_ID, "Shirt", PRICE, "MXN", 10);

        lp.incrementStockSold();
        lp.incrementStockSold();

        assertThat(lp.getStockSold()).isEqualTo(2);
    }
}
