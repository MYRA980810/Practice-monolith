package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.application.port.in.ProductFilter;
import com.livecomerce.catalog.domain.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link ProductPersistenceAdapter#loadByFilter} against a real
 * PostgreSQL instance (via docker-compose, Flyway-migrated) — same rationale
 * as {@code ChannelMetricsRepositoryTest}: a Specification-composition bug
 * cannot be verified with mocks, only with a real query.
 *
 * Regression guard: {@code loadByFilter} is reachable from the public
 * unauthenticated {@code GET /api/products?storeId=&categoryId=} endpoint and
 * must exclude paused products just like the no-categoryId sibling path
 * ({@code findByStoreIdActiveWithDetails}) already does.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProductPersistenceAdapter.class)
class ProductPersistenceAdapterTest {

    @Autowired ProductPersistenceAdapter adapter;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void loadByFilter_withCategoryId_excludesPausedProducts() {
        var storeId = seedStore();
        var categoryId = seedCategory();

        var activeProduct = Product.create(storeId, "Active Product", "desc",
                new BigDecimal("10.00"), "MXN", "SKU-ACTIVE-" + storeId, categoryId);
        entityManager.persist(activeProduct);

        var pausedProduct = Product.create(storeId, "Paused Product", "desc",
                new BigDecimal("10.00"), "MXN", "SKU-PAUSED-" + storeId, categoryId);
        entityManager.persist(pausedProduct);
        pausedProduct.pause();

        entityManager.flush();
        entityManager.clear();

        var filter = new ProductFilter(storeId, categoryId, null, null);
        var results = adapter.loadByFilter(filter);

        assertThat(results).extracting(Product::getId)
                .containsExactly(activeProduct.getId());
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
