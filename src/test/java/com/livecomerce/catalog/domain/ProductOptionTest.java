package com.livecomerce.catalog.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductOptionTest {

    private static Product buildProduct() {
        return Product.create(UUID.randomUUID(), "Shirt", null, BigDecimal.TEN, "MXN", "SKU-1", null);
    }

    @Test
    void of_setsNameAndPositionAndBuildsValues() {
        var product = buildProduct();
        var option  = ProductOption.of(product, "Color", 0, List.of("Red", "Blue", "Green"));

        assertThat(option.getId()).isNotNull();
        assertThat(option.getName()).isEqualTo("Color");
        assertThat(option.getPosition()).isEqualTo(0);
        assertThat(option.getProduct()).isEqualTo(product);
        assertThat(option.getValues()).hasSize(3);
        assertThat(option.getValues().get(0).getValue()).isEqualTo("Red");
        assertThat(option.getValues().get(1).getValue()).isEqualTo("Blue");
        assertThat(option.getValues().get(2).getValue()).isEqualTo("Green");
    }

    @Test
    void of_assignsPositionToValues() {
        var option = ProductOption.of(buildProduct(), "Size", 0, List.of("S", "M", "L"));

        assertThat(option.getValues().get(0).getPosition()).isEqualTo(0);
        assertThat(option.getValues().get(1).getPosition()).isEqualTo(1);
        assertThat(option.getValues().get(2).getPosition()).isEqualTo(2);
    }

    @Test
    void findValue_returnsCorrectValue() {
        var option = ProductOption.of(buildProduct(), "Color", 0, List.of("Red", "Blue"));

        var found = option.findValue("Red");
        assertThat(found.getValue()).isEqualTo("Red");
    }

    @Test
    void findValue_whenNotFound_throwsIllegalArgument() {
        var option = ProductOption.of(buildProduct(), "Color", 0, List.of("Red"));

        assertThatThrownBy(() -> option.findValue("Green"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void persistable_isNew_trueOnCreate() {
        var option = ProductOption.of(buildProduct(), "Color", 0, List.of("Red"));
        assertThat(option.isNew()).isTrue();
    }
}
