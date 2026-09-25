package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Exercises the JPQL of {@link ProductJpaRepository#countActiveByStoreAndCategory}
 * against a real PostgreSQL instance (Flyway-migrated). {@code StoreCategoryAdapterTest}
 * mocks the repository, so the query's filters can only be verified here.
 * Default {@code @DataJpaTest} transactional rollback leaves no committed rows.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductJpaRepositoryStoreCategoryCountTest {

    @Autowired ProductJpaRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void countsVisibleProductsGroupedPerStoreAndCategory() {
        var storeA = seedStore();
        var storeB = seedStore();
        var cat1 = seedCategory();
        var cat2 = seedCategory();

        persist(storeA, cat1);
        persist(storeA, cat1);
        persist(storeA, cat2);
        persist(storeB, cat2);
        flushAndClear();

        var rows = repository.countActiveByStoreAndCategory(List.of(storeA, storeB));

        assertThat(rows).extracting(r -> r[0], r -> r[1], r -> ((Number) r[2]).longValue())
                .containsExactlyInAnyOrder(
                        tuple(storeA, cat1, 2L),
                        tuple(storeA, cat2, 1L),
                        tuple(storeB, cat2, 1L));
    }

    @Test
    void excludesPausedProducts() {
        var storeId = seedStore();
        var categoryId = seedCategory();

        persist(storeId, categoryId);
        persist(storeId, categoryId).pause();
        flushAndClear();

        var rows = repository.countActiveByStoreAndCategory(List.of(storeId));

        assertThat(rows).extracting(r -> r[0], r -> r[1], r -> ((Number) r[2]).longValue())
                .containsExactly(tuple(storeId, categoryId, 1L));
    }

    @Test
    void excludesInactiveProducts() {
        var storeId = seedStore();
        var categoryId = seedCategory();

        persist(storeId, categoryId);
        persist(storeId, categoryId).deactivate();
        flushAndClear();

        var rows = repository.countActiveByStoreAndCategory(List.of(storeId));

        assertThat(rows).extracting(r -> r[0], r -> r[1], r -> ((Number) r[2]).longValue())
                .containsExactly(tuple(storeId, categoryId, 1L));
    }

    @Test
    void excludesProductsWithoutCategory() {
        var storeId = seedStore();

        persist(storeId, null);
        flushAndClear();

        var rows = repository.countActiveByStoreAndCategory(List.of(storeId));

        assertThat(rows).isEmpty();
    }

    @Test
    void onlyIncludesRequestedStores() {
        var requested = seedStore();
        var other = seedStore();
        var categoryId = seedCategory();

        persist(requested, categoryId);
        persist(other, categoryId);
        flushAndClear();

        var rows = repository.countActiveByStoreAndCategory(List.of(requested));

        assertThat(rows).extracting(r -> r[0], r -> r[1], r -> ((Number) r[2]).longValue())
                .containsExactly(tuple(requested, categoryId, 1L));
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
