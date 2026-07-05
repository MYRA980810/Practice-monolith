package com.livecomerce.analytics.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link ChannelMetricsRepository#findLivePerformances} against a
 * real PostgreSQL instance (via {@code docker-compose}, Flyway-migrated) —
 * a fan-out bug caused by two independent LEFT JOINs off the same parent
 * (order_items and live_products both joined to lives) cannot be verified
 * with mocks, only with real SQL execution. Each test runs inside a rolled
 * back transaction ({@code @DataJpaTest} default), so no dev data is left
 * behind.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChannelMetricsRepositoryTest {

    @Autowired ChannelMetricsRepository repository;
    @Autowired JdbcTemplate jdbc;

    @Test
    void findLivePerformances_multiProductMultiItemLive_doesNotMultiplyRevenueUnitsOrAllocation() {
        var store = seedStore();
        var storeId = store.storeId();
        var liveId = seedLive(storeId, store.sellerId(), "Multi live", 10, OffsetDateTime.now().minusHours(1));

        // 3 live_products rows: stock_allocated 10, 20, 5 => total 35
        seedLiveProduct(liveId, storeId, 10);
        seedLiveProduct(liveId, storeId, 20);
        seedLiveProduct(liveId, storeId, 5);

        // 2 PAID order_items across 2 orders: subtotals 40 (qty 4) and 60 (qty 6) => revenue 100, units 10
        seedPaidOrderWithItem(storeId, liveId, new BigDecimal("40.00"), 4);
        seedPaidOrderWithItem(storeId, liveId, new BigDecimal("60.00"), 6);

        var from = OffsetDateTime.now().minusDays(1);
        var to = OffsetDateTime.now().plusDays(1);
        var rows = repository.findLivePerformances(storeId, from, to);

        assertThat(rows).hasSize(1);
        var row = rows.get(0);
        assertThat((UUID) row[0]).isEqualTo(liveId);
        assertThat(toBigDecimal(row[2])).isEqualByComparingTo("100.00");
        assertThat(((Number) row[3]).longValue()).isEqualTo(10L);
        assertThat(((Number) row[5]).longValue()).isEqualTo(35L);
    }

    @Test
    void findLivePerformances_singleProductSingleItemLive_matchesPreFixValues() {
        var store = seedStore();
        var storeId = store.storeId();
        var liveId = seedLive(storeId, store.sellerId(), "Single live", 7, OffsetDateTime.now().minusHours(2));

        seedLiveProduct(liveId, storeId, 10);
        seedPaidOrderWithItem(storeId, liveId, new BigDecimal("40.00"), 4);

        var from = OffsetDateTime.now().minusDays(1);
        var to = OffsetDateTime.now().plusDays(1);
        var rows = repository.findLivePerformances(storeId, from, to);

        assertThat(rows).hasSize(1);
        var row = rows.get(0);
        assertThat(toBigDecimal(row[2])).isEqualByComparingTo("40.00");
        assertThat(((Number) row[3]).longValue()).isEqualTo(4L);
        assertThat(((Number) row[5]).longValue()).isEqualTo(10L);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    // --- seeding helpers (respect FK constraints: users -> stores -> products -> lives/orders) ---

    private record SeededStore(UUID storeId, UUID sellerId) {}

    private SeededStore seedStore() {
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
        return new SeededStore(storeId, userId);
    }

    private UUID seedLive(UUID storeId, UUID sellerId, String title, int peakViewers, OffsetDateTime endedAt) {
        var liveId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO lives (id, store_id, seller_id, title, status, peak_viewers, ended_at)
                VALUES (?, ?, ?, ?, 'ENDED', ?, ?)
                """, liveId, storeId, sellerId, title, peakViewers, endedAt);
        return liveId;
    }

    private record SeededProduct(UUID productId, UUID variantId) {}

    private SeededProduct seedProduct(UUID storeId) {
        var productId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO products (id, store_id, name, base_price)
                VALUES (?, ?, 'Test Product', 10.00)
                """, productId, storeId);

        var variantId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product_variants (id, product_id)
                VALUES (?, ?)
                """, variantId, productId);
        return new SeededProduct(productId, variantId);
    }

    private void seedLiveProduct(UUID liveId, UUID storeId, int stockAllocated) {
        var product = seedProduct(storeId);
        jdbc.update("""
                INSERT INTO live_products (id, live_id, product_id, variant_id, product_name_snapshot,
                    price_snapshot, stock_allocated)
                VALUES (?, ?, ?, ?, 'Test Product', 10.00, ?)
                """, UUID.randomUUID(), liveId, product.productId(), product.variantId(), stockAllocated);
    }

    private void seedPaidOrderWithItem(UUID storeId, UUID liveId, BigDecimal subtotal, int quantity) {
        var buyerId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, role, first_name, last_name)
                VALUES (?, ?, 'hash', 'BUYER', 'Buyer', 'Test')
                """, buyerId, "buyer-" + buyerId + "@test.com");

        var orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, buyer_id, live_id, store_id, status)
                VALUES (?, ?, ?, ?, 'PAID')
                """, orderId, buyerId, liveId, storeId);

        var product = seedProduct(storeId);
        jdbc.update("""
                INSERT INTO order_items (id, order_id, product_id, variant_id, product_name, unit_price,
                    quantity, subtotal, status)
                VALUES (?, ?, ?, ?, 'Test Product', ?, ?, ?, 'PAID')
                """, UUID.randomUUID(), orderId, product.productId(), product.variantId(),
                subtotal.divide(BigDecimal.valueOf(quantity), 2, java.math.RoundingMode.HALF_UP),
                quantity, subtotal);
    }
}
