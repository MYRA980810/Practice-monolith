package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.CategoryStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryPersistenceAdapterTest {

    @Mock CategoryJpaRepository repository;

    @InjectMocks CategoryPersistenceAdapter adapter;

    @Test
    void loadAllActive_returnsActiveCategories() {
        var c1 = Category.create("Electrónica", "electronica", null);
        var c2 = Category.create("Moda Femenina", "moda-femenina", null);
        when(repository.findAllByStatusOrderedForDisplay(CategoryStatus.ACTIVE)).thenReturn(List.of(c1, c2));

        var result = adapter.loadAllActive();

        assertThat(result).containsExactly(c1, c2);
    }
}
