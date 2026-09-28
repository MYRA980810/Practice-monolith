package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.CategoryStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the JPQL of {@link CategoryJpaRepository#findAllByStatusOrderedForDisplay}
 * against a real PostgreSQL instance (Flyway-migrated, including the V59 seed ordering).
 * The database is shared, so assertions are relative (order among the rows seeded here
 * and the V16 seeds) rather than on the exact full list.
 * Default {@code @DataJpaTest} transactional rollback leaves no committed rows.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryJpaRepositoryActiveOrderTest {

    private static final List<String> FEATURED_SEEDS = List.of(
            "moda-femenina", "moda-masculina", "belleza", "accesorios-moda",
            "calzado", "joyeria-relojes", "electronica", "hogar-decoracion");

    @Autowired CategoryJpaRepository repository;
    @Autowired JdbcTemplate jdbc;

    @Test
    void ordersByDisplayOrderBeforeName() {
        var first = seedCategory("B Category", 100_001, "ACTIVE");
        var second = seedCategory("A Category", 100_002, "ACTIVE");

        var ids = activeIds();

        assertThat(ids.indexOf(first)).isNotNegative().isLessThan(ids.indexOf(second));
    }

    @Test
    void placesUnorderedCategoriesLastSortedByName() {
        var ordered = seedCategory("Z Category", 100_001, "ACTIVE");
        var unorderedB = seedCategory("ZZZ Unordered B " + UUID.randomUUID(), null, "ACTIVE");
        var unorderedA = seedCategory("ZZZ Unordered A " + UUID.randomUUID(), null, "ACTIVE");

        var ids = activeIds();

        assertThat(ids.indexOf(ordered)).isNotNegative().isLessThan(ids.indexOf(unorderedA));
        assertThat(ids.indexOf(unorderedA)).isLessThan(ids.indexOf(unorderedB));
        var categories = repository.findAllByStatusOrderedForDisplay(CategoryStatus.ACTIVE);
        var lastOrderedIndex = lastIndexWithDisplayOrder(categories);
        assertThat(ids.indexOf(unorderedA)).isGreaterThan(lastOrderedIndex);
    }

    @Test
    void excludesCategoriesNotInRequestedStatus() {
        var pending = seedCategory("Pending Category", 1, "PENDING_REVIEW");

        assertThat(activeIds()).doesNotContain(pending);
    }

    @Test
    void featuredSeedsComeFirstInEditorialOrder() {
        var categories = repository.findAllByStatusOrderedForDisplay(CategoryStatus.ACTIVE);

        var featuredSlugs = categories.stream()
                .filter(Category::isFeatured)
                .map(Category::getSlug)
                .toList();

        assertThat(featuredSlugs).containsExactlyElementsOf(FEATURED_SEEDS);
        assertThat(categories.stream().limit(FEATURED_SEEDS.size()).map(Category::getSlug).toList())
                .containsExactlyElementsOf(FEATURED_SEEDS);
    }

    @Test
    void otroIsTheLastSeededCategory() {
        var categories = repository.findAllByStatusOrderedForDisplay(CategoryStatus.ACTIVE);

        var otro = categories.stream().filter(c -> c.getSlug().equals("otro")).findFirst().orElseThrow();

        assertThat(otro.isFeatured()).isFalse();
        assertThat(categories.indexOf(otro)).isEqualTo(lastIndexWithDisplayOrder(categories));
    }

    private List<UUID> activeIds() {
        return repository.findAllByStatusOrderedForDisplay(CategoryStatus.ACTIVE).stream()
                .map(Category::getId)
                .toList();
    }

    private static int lastIndexWithDisplayOrder(List<Category> categories) {
        var last = -1;
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getDisplayOrder() != null) {
                last = i;
            }
        }
        return last;
    }

    private UUID seedCategory(String name, Integer displayOrder, String status) {
        var categoryId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO categories (id, name, slug, status, display_order)
                VALUES (?, ?, ?, ?, ?)
                """, categoryId, name, "test-category-" + categoryId, status, displayOrder);
        return categoryId;
    }
}
