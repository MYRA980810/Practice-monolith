package com.livecomerce.catalog.domain;

import com.livecomerce.catalog.application.DuplicateVariantException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private static final UUID STORE_ID    = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @Test
    void create_withCategoryId_setsCategoryId() {
        var product = Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, CATEGORY_ID);
        assertThat(product.getCategoryId()).isEqualTo(CATEGORY_ID);
    }

    @Test
    void create_withNullCategoryId_categoryIdIsNull() {
        var product = Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
        assertThat(product.getCategoryId()).isNull();
    }

    @Test
    void create_buildsDefaultVariantWithSku() {
        var product = Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", "SKU-001", null);

        assertThat(product.getVariants()).hasSize(1);
        var dv = product.defaultVariant();
        assertThat(dv.isDefault()).isTrue();
        assertThat(dv.getSku()).isEqualTo("SKU-001");
        assertThat(dv.getStock()).isNotNull();
    }

    @Test
    void assignCategory_updatesCategoryId() {
        var product = Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
        product.assignCategory(CATEGORY_ID);
        assertThat(product.getCategoryId()).isEqualTo(CATEGORY_ID);
    }

    @Test
    void addOption_appendsOptionWithValues() {
        var product = Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
        var option  = product.addOption("Color", List.of("Red", "Blue"));

        assertThat(product.getOptions()).hasSize(1);
        assertThat(option.getName()).isEqualTo("Color");
        assertThat(option.getValues()).hasSize(2);
    }

    @Test
    void addVariant_createsNonDefaultVariant() {
        var product = Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
        product.addOption("Color", List.of("Red", "Blue"));

        var variant = product.addVariant(Map.of("Color", "Red"), "SKU-RED", new BigDecimal("15.00"));

        assertThat(product.getVariants()).hasSize(2);
        assertThat(variant.isDefault()).isFalse();
        assertThat(variant.getSku()).isEqualTo("SKU-RED");
    }

    @Test
    void addVariant_withDuplicateCombination_throwsDuplicateVariantException() {
        var product = Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
        product.addOption("Color", List.of("Red"));
        product.addVariant(Map.of("Color", "Red"), "SKU-RED", null);

        assertThatThrownBy(() -> product.addVariant(Map.of("Color", "Red"), "SKU-RED-2", null))
                .isInstanceOf(DuplicateVariantException.class);
    }

    @Test
    void addVariant_withMissingOption_throwsIllegalArgument() {
        var product = Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
        product.addOption("Color", List.of("Red"));

        assertThatThrownBy(() -> product.addVariant(Map.of(), "SKU", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing option");
    }

    @Test
    void stockDelegates_routeToDefaultVariant() {
        var product = Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
        product.addStock(10);

        var stock = product.defaultVariant().getStock();
        assertThat(stock.getTotalQuantity()).isEqualTo(10);
        assertThat(stock.getAvailableQuantity()).isEqualTo(10);
    }

    @Test
    void update_routesSkuToDefaultVariant() {
        var product = Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", "OLD", null);
        product.update("Remera 2", null, new BigDecimal("12.00"), "MXN", "NEW-SKU");

        assertThat(product.defaultVariant().getSku()).isEqualTo("NEW-SKU");
        assertThat(product.getName()).isEqualTo("Remera 2");
    }
}
