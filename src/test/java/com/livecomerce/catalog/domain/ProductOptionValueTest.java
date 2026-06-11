package com.livecomerce.catalog.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
}
