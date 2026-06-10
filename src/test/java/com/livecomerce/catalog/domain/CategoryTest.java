package com.livecomerce.catalog.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    @Test
    void create_setsStatusActive() {
        var category = Category.create("Electrónica", "electronica", null);
        assertThat(category.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
    }

    @Test
    void create_generatesId() {
        var category = Category.create("Electrónica", "electronica", null);
        assertThat(category.getId()).isNotNull();
    }

    @Test
    void create_setsIsNewTrue() {
        var category = Category.create("Electrónica", "electronica", null);
        assertThat(category.isNew()).isTrue();
    }
}
