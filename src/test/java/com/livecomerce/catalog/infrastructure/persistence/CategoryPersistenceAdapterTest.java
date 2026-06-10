package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.CategoryStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryPersistenceAdapterTest {

    @Mock CategoryJpaRepository repository;

    @InjectMocks CategoryPersistenceAdapter adapter;

    @Test
    @SuppressWarnings("null")
    void save_delegatesToRepository() {
        var category = Category.create("Electrónica", "electronica", null);
        when(repository.save(any())).thenReturn(category);

        var result = adapter.save(category);

        verify(repository).save(category);
        assertThat(result).isEqualTo(category);
    }

    @Test
    void loadAllActive_returnsActiveCategories() {
        var c1 = Category.create("Electrónica", "electronica", null);
        var c2 = Category.create("Moda Femenina", "moda-femenina", null);
        when(repository.findAllByStatus(CategoryStatus.ACTIVE)).thenReturn(List.of(c1, c2));

        var result = adapter.loadAllActive();

        assertThat(result).hasSize(2);
    }

    @Test
    void findBySlug_returnsEmpty_whenNotFound() {
        when(repository.findBySlug("no-such-slug")).thenReturn(Optional.empty());

        var result = adapter.findBySlug("no-such-slug");

        assertThat(result).isEmpty();
    }
}
