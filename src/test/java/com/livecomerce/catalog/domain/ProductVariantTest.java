package com.livecomerce.catalog.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductVariantTest {

    private static Product buildProduct() {
        return Product.create(UUID.randomUUID(), "Shirt", null, BigDecimal.TEN, "MXN", "SKU-1", null);
    }

    @Test
    void createDefault_hasIsDefaultTrue_andEmptyStock() {
        var product = buildProduct();
        var variant = ProductVariant.createDefault(product, "SKU-DEFAULT");

        assertThat(variant.isDefault()).isTrue();
        assertThat(variant.getSku()).isEqualTo("SKU-DEFAULT");
        assertThat(variant.getPosition()).isEqualTo(0);
        assertThat(variant.getStock()).isNotNull();
        assertThat(variant.getStock().getTotalQuantity()).isEqualTo(0);
        assertThat(variant.isNew()).isTrue();
    }

    @Test
    void create_hasIsDefaultFalse_withOptionValues() {
        var product = buildProduct();
        var option  = ProductOption.of(product, "Color", 0, List.of("Red"));
        var ov      = option.getValues().getFirst();

        var variant = ProductVariant.create(product, "SKU-RED", new BigDecimal("15.00"), Set.of(ov), 1);

        assertThat(variant.isDefault()).isFalse();
        assertThat(variant.getSku()).isEqualTo("SKU-RED");
        assertThat(variant.getPriceOverride()).isEqualByComparingTo("15.00");
        assertThat(variant.getOptionValues()).containsExactly(ov);
        assertThat(variant.isNew()).isTrue();
    }

    @Test
    void effectivePrice_withOverride_returnsOverride() {
        var product = buildProduct();
        var variant = ProductVariant.create(product, null, new BigDecimal("20.00"), Set.of(), 1);

        assertThat(variant.effectivePrice(BigDecimal.TEN)).isEqualByComparingTo("20.00");
    }

    @Test
    void effectivePrice_withoutOverride_returnsBasePrice() {
        var product = buildProduct();
        var variant = ProductVariant.createDefault(product, null);

        assertThat(variant.effectivePrice(BigDecimal.TEN)).isEqualByComparingTo("10.00");
    }

    @Test
    void addStock_delegatesToStockAndUpdates() {
        var product = buildProduct();
        var variant = ProductVariant.createDefault(product, null);

        variant.addStock(10);

        assertThat(variant.getStock().getTotalQuantity()).isEqualTo(10);
        assertThat(variant.getStock().getAvailableQuantity()).isEqualTo(10);
    }

    @Test
    void canReserve_whenSufficient_returnsTrue() {
        var product = buildProduct();
        var variant = ProductVariant.createDefault(product, null);
        variant.addStock(5);

        assertThat(variant.canReserve(5)).isTrue();
    }

    @Test
    void reserveAndSell_updateStockCorrectly() {
        var product = buildProduct();
        var variant = ProductVariant.createDefault(product, null);
        variant.addStock(10);
        variant.reserveStock(3);
        variant.sellStock(3);

        assertThat(variant.getStock().getTotalQuantity()).isEqualTo(7);
        assertThat(variant.getStock().getReservedQuantity()).isEqualTo(0);
    }

    @Test
    void releaseStock_movesReservedBackToAvailable() {
        var product = buildProduct();
        var variant = ProductVariant.createDefault(product, null);
        variant.addStock(10);
        variant.reserveStock(4);
        variant.releaseStock(2);

        assertThat(variant.getStock().getAvailableQuantity()).isEqualTo(8);
        assertThat(variant.getStock().getReservedQuantity()).isEqualTo(2);
    }

    @Test
    void updateSku_changesSku() {
        var product = buildProduct();
        var variant = ProductVariant.createDefault(product, "OLD-SKU");
        variant.updateSku("NEW-SKU");

        assertThat(variant.getSku()).isEqualTo("NEW-SKU");
        assertThat(variant.getUpdatedAt()).isNotNull();
    }
}
