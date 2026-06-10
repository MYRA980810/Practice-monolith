package com.livecomerce.catalog.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
    void assignCategory_updatesCategoryId() {
        var product = Product.create(STORE_ID, "Remera", null, BigDecimal.TEN, "MXN", null, null);
        product.assignCategory(CATEGORY_ID);
        assertThat(product.getCategoryId()).isEqualTo(CATEGORY_ID);
    }
}
