package com.livecomerce.catalog.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductOptionValueTest {

    @Test
    void of_setsFieldsCorrectly() {
        var product = Product.create(UUID.randomUUID(), "Shirt", null, BigDecimal.TEN, "MXN", "SKU-1", null);
        var option  = ProductOption.of(product, "Color", 0, List.of("Red", "Blue"));
        var value   = option.getValues().getFirst();

        assertThat(value.getId()).isNotNull();
        assertThat(value.getValue()).isEqualTo("Red");
        assertThat(value.getPosition()).isEqualTo(0);
        assertThat(value.getOption()).isEqualTo(option);
    }

    @Test
    void persistable_isNew_trueOnCreate() {
        var product = Product.create(UUID.randomUUID(), "Shirt", null, BigDecimal.TEN, "MXN", "SKU-1", null);
        var option  = ProductOption.of(product, "Size", 0, List.of("S"));
        var value   = option.getValues().getFirst();

        assertThat(value.isNew()).isTrue();
    }

    // --- swatchHex ---

    @Test
    void swatchHex_defaultsToNull_whenNeverAssigned() {
        var product = Product.create(UUID.randomUUID(), "Shirt", null, BigDecimal.TEN, "MXN", "SKU-1", null);
        var option  = ProductOption.of(product, "Size", 0, List.of("S"));
        var value   = option.getValues().getFirst();

        assertThat(value.getSwatchHex()).isNull();
    }

    @Test
    void assignSwatchHex_withValidHex_setsField() {
        var product = Product.create(UUID.randomUUID(), "Shirt", null, BigDecimal.TEN, "MXN", "SKU-1", null);
        var option  = ProductOption.of(product, "Color", 0, List.of("Red"));
        var value   = option.getValues().getFirst();

        value.assignSwatchHex("#FF0000");

        assertThat(value.getSwatchHex()).isEqualTo("#FF0000");
    }

    @Test
    void assignSwatchHex_withNull_clearsField() {
        var product = Product.create(UUID.randomUUID(), "Shirt", null, BigDecimal.TEN, "MXN", "SKU-1", null);
        var option  = ProductOption.of(product, "Color", 0, List.of("Red"));
        var value   = option.getValues().getFirst();

        value.assignSwatchHex("#FF0000");
        value.assignSwatchHex(null);

        assertThat(value.getSwatchHex()).isNull();
    }

    @Test
    void assignSwatchHex_withInvalidFormat_throwsIllegalArgumentException() {
        var product = Product.create(UUID.randomUUID(), "Shirt", null, BigDecimal.TEN, "MXN", "SKU-1", null);
        var option  = ProductOption.of(product, "Color", 0, List.of("Red"));
        var value   = option.getValues().getFirst();

        assertThatThrownBy(() -> value.assignSwatchHex("red"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
