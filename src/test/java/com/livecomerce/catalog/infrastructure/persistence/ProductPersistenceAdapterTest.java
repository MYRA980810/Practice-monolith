package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.application.port.in.ProductFilter;
import com.livecomerce.catalog.domain.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
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

    // --- browsePublic ---

    @Test
    void browsePublic_withoutCategoryId_includesActiveNonPausedProductsAcrossStores() {
        var storeA = seedStore();
        var storeB = seedStore();
        var categoryId = seedCategory();

        var productA = Product.create(storeA, "Product A", "desc", new BigDecimal("10.00"), "MXN", "SKU-A-" + storeA, categoryId);
        entityManager.persist(productA);
        var productB = Product.create(storeB, "Product B", "desc", new BigDecimal("20.00"), "MXN", "SKU-B-" + storeB, categoryId);
        entityManager.persist(productB);
        entityManager.flush();
        entityManager.clear();

        var filter = new ProductFilter(null, null, null, null);
        // No categoryId means the query is intentionally unscoped across the whole
        // catalog, so the shared dev database may hold unrelated leftover rows.
        // RECENTLY_ADDED sort puts these just-created products first; assert
        // presence (not exclusivity) and use a generous page size as margin.
        var page = adapter.browsePublic(filter, PageRequest.of(0, 50));

        assertThat(page.getContent()).extracting(Product::getId)
                .contains(productA.getId(), productB.getId());
    }

    @Test
    void browsePublic_withCategoryId_filtersByCategory() {
        var storeId = seedStore();
        var matchingCategory = seedCategory();
        var otherCategory = seedCategory();

        var matching = Product.create(storeId, "Matching", "desc", new BigDecimal("10.00"), "MXN", "SKU-MATCH-" + storeId, matchingCategory);
        entityManager.persist(matching);
        var nonMatching = Product.create(storeId, "Non-matching", "desc", new BigDecimal("10.00"), "MXN", "SKU-OTHER-" + storeId, otherCategory);
        entityManager.persist(nonMatching);
        entityManager.flush();
        entityManager.clear();

        var filter = new ProductFilter(null, matchingCategory, null, null);
        var page = adapter.browsePublic(filter, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Product::getId)
                .containsExactly(matching.getId());
    }

    @Test
    void browsePublic_excludesPausedProducts() {
        var storeId = seedStore();
        var categoryId = seedCategory();

        var active = Product.create(storeId, "Active", "desc", new BigDecimal("10.00"), "MXN", "SKU-ACTIVE-" + storeId, categoryId);
        entityManager.persist(active);
        var paused = Product.create(storeId, "Paused", "desc", new BigDecimal("10.00"), "MXN", "SKU-PAUSED-" + storeId, categoryId);
        entityManager.persist(paused);
        paused.pause();
        entityManager.flush();
        entityManager.clear();

        var filter = new ProductFilter(null, categoryId, null, null);
        var page = adapter.browsePublic(filter, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Product::getId)
                .containsExactly(active.getId());
    }

    @Test
    void browsePublic_excludesInactiveProducts() {
        var storeId = seedStore();
        var categoryId = seedCategory();

        var active = Product.create(storeId, "Active", "desc", new BigDecimal("10.00"), "MXN", "SKU-ACTIVE2-" + storeId, categoryId);
        entityManager.persist(active);
        var deactivated = Product.create(storeId, "Deactivated", "desc", new BigDecimal("10.00"), "MXN", "SKU-DEACTIVATED-" + storeId, categoryId);
        entityManager.persist(deactivated);
        deactivated.deactivate();
        entityManager.flush();
        entityManager.clear();

        var filter = new ProductFilter(null, categoryId, null, null);
        var page = adapter.browsePublic(filter, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Product::getId)
                .containsExactly(active.getId());
    }

    @Test
    void browsePublic_neverAppliesStoreFilter() {
        var storeId = seedStore();
        var categoryId = seedCategory();

        var product = Product.create(storeId, "Product", "desc", new BigDecimal("10.00"), "MXN", "SKU-ANY-" + storeId, categoryId);
        entityManager.persist(product);
        entityManager.flush();
        entityManager.clear();

        // storeId is set on the filter but browsePublic must ignore it entirely
        var filter = new ProductFilter(UUID.randomUUID(), categoryId, null, null);
        var page = adapter.browsePublic(filter, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Product::getId)
                .containsExactly(product.getId());
    }

    // --- compareAtPrice ---

    @Test
    void compareAtPrice_persistsAndReloadsAsSet() {
        var storeId = seedStore();
        var product = Product.create(storeId, "Discounted", "desc",
                new BigDecimal("75.00"), "MXN", "SKU-DISC-" + storeId, null);
        product.updateCompareAtPrice(new BigDecimal("100.00"));
        entityManager.persist(product);
        entityManager.flush();
        entityManager.clear();

        var reloaded = entityManager.find(Product.class, product.getId());

        assertThat(reloaded.getCompareAtPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void compareAtPrice_persistsAndReloadsAsNullWhenNeverSet() {
        var storeId = seedStore();
        var product = Product.create(storeId, "Regular", "desc",
                new BigDecimal("75.00"), "MXN", "SKU-REG-" + storeId, null);
        entityManager.persist(product);
        entityManager.flush();
        entityManager.clear();

        var reloaded = entityManager.find(Product.class, product.getId());

        assertThat(reloaded.getCompareAtPrice()).isNull();
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
