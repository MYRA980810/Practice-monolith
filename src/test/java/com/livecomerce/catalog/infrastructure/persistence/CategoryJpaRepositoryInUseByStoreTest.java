package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the JPQL of {@link CategoryJpaRepository#findCategoriesInUseByStore}
 * against a real PostgreSQL instance (Flyway-migrated). The seller's product list
 * ({@code GET /api/products/me}) only shows active, non-paused products, so the
 * category tabs derived from this query must follow the same rule.
 * Default {@code @DataJpaTest} transactional rollback leaves no committed rows.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null")
class CategoryJpaRepositoryInUseByStoreTest {

    @Autowired CategoryJpaRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void returnsDistinctCategoriesOfVisibleProducts() {
        var storeId = seedStore();
        var cat1 = seedCategory();
        var cat2 = seedCategory();

        persist(storeId, cat1);
        persist(storeId, cat1);
        persist(storeId, cat2);
        flushAndClear();

        var categories = repository.findCategoriesInUseByStore(storeId);

        assertThat(categories).extracting(Category::getId).containsExactlyInAnyOrder(cat1, cat2);
    }

    @Test
    void excludesCategoriesWithOnlyPausedProducts() {
        var storeId = seedStore();
        var visible = seedCategory();
        var pausedOnly = seedCategory();

        persist(storeId, visible);
        persist(storeId, pausedOnly).pause();
        flushAndClear();

        var categories = repository.findCategoriesInUseByStore(storeId);

        assertThat(categories).extracting(Category::getId).containsExactly(visible);
    }

    @Test
    void excludesCategoriesWithOnlyInactiveProducts() {
        var storeId = seedStore();
        var visible = seedCategory();
        var inactiveOnly = seedCategory();

        persist(storeId, visible);
        persist(storeId, inactiveOnly).deactivate();
        flushAndClear();

        var categories = repository.findCategoriesInUseByStore(storeId);

        assertThat(categories).extracting(Category::getId).containsExactly(visible);
    }

    @Test
    void ignoresOtherStores() {
        var storeId = seedStore();
        var otherStore = seedStore();
        var categoryId = seedCategory();

        persist(otherStore, categoryId);
        flushAndClear();

        assertThat(repository.findCategoriesInUseByStore(storeId)).isEmpty();
    }

    private Product persist(UUID storeId, UUID categoryId) {
        var product = Product.create(storeId, "Product", "desc", new BigDecimal("10.00"), "MXN",
                "SKU-" + UUID.randomUUID(), categoryId);
        entityManager.persist(product);
        return product;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private UUID seedStore() {
        var userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, role, first_name, last_name)
                VALUES (?, ?, 'hash', 'SELLER', 'Seller', 'Test')
                """, userId, "seller-" + userId + "@test.com");

        var storeId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO stores (id, user_id, name, slug)
                VALUES (?, ?, 'Test Store', ?)
                """, storeId, userId, "store-" + storeId);
        return storeId;
    }

    private UUID seedCategory() {
        var categoryId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO categories (id, name, slug)
                VALUES (?, 'Test Category', ?)
                """, categoryId, "test-category-" + categoryId);
        return categoryId;
    }
}
