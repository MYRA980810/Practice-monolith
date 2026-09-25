package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.CategoryStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveCategoryLookupAdapterTest {

    @Mock CategoryJpaRepository repository;

    @InjectMocks LiveCategoryLookupAdapter adapter;

    @Test
    void isActive_activeCategory_returnsTrue() {
        var category = Category.create("Moda", "moda", null);
        when(repository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThat(adapter.isActive(category.getId())).isTrue();
    }

    @Test
    void isActive_pendingReviewCategory_returnsFalse() {
        var category = Category.create("Moda", "moda", null);
        ReflectionTestUtils.setField(category, "status", CategoryStatus.PENDING_REVIEW);
        when(repository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThat(adapter.isActive(category.getId())).isFalse();
    }

    @Test
    void isActive_missingCategory_returnsFalse() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThat(adapter.isActive(id)).isFalse();
    }
}
