package com.livecomerce.catalog.domain;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    void create_isNotFeaturedAndHasNoDisplayOrder() {
        var category = Category.create("Electrónica", "electronica", null);
        assertThat(category.isFeatured()).isFalse();
        assertThat(category.getDisplayOrder()).isNull();
    }

    @Test
    void isActive_trueWhenStatusActive() {
        var category = Category.create("Electrónica", "electronica", null);
        assertThat(category.isActive()).isTrue();
    }

    @Test
    void isActive_falseWhenPendingReview() {
        var category = Category.create("Electrónica", "electronica", null);
        ReflectionTestUtils.setField(category, "status", CategoryStatus.PENDING_REVIEW);
        assertThat(category.isActive()).isFalse();
    }
}
