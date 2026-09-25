package com.livecomerce.catalog.application;

import com.livecomerce.catalog.LoadCartProductInfoPort;
import com.livecomerce.catalog.LoadCartProductInfoPort.CartLineRef;
import com.livecomerce.catalog.LoadLiveProductStatusPort;
import com.livecomerce.catalog.domain.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.TestTransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for a real production bug: {@code loadForCart} threw
 * {@code org.hibernate.LazyInitializationException} on {@code Product.images}
 * during add-to-cart, because the method was missing {@code @Transactional}.
 * Without it, {@code ProductPersistenceAdapter.loadByIds}'s "hydrate lazy
 * collections in the same session" trick (two separate repository calls, one
 * for variants/stock and one for images) runs across two different Hibernate
 * sessions instead of sharing one — the returned {@code Product.images}
 * collection stays an uninitialized proxy bound to an already-closed session.
 *
 * <p>{@link CartProductInfoAdapterTest} (Mockito-based) cannot catch this
 * class of bug: its {@code Product} fixtures never touch Hibernate, so no
 * lazy proxy and no session are ever involved. This test goes through the
 * real Spring-proxied {@code CartProductInfoAdapter} bean (calling {@code
 * new CartProductInfoAdapter(...)} directly would silently skip its
 * {@code @Transactional} advice — AOP proxying only applies to
 * container-managed beans) wired to the real {@code ProductPersistenceAdapter}
 * against Postgres, and uses {@link TestTransaction#end()} to close the setup
 * session before calling the port under test — matching {@code
 * spring.jpa.open-in-view: false} in production, where no request-scoped
 * session is left open either.
 *
 * <p><b>{@code TestTransaction.end()} commits for real</b> — unlike
 * {@code @DataJpaTest}'s default rollback, this data survives past the test
 * and would otherwise leak into whatever database {@code
 * spring.datasource.url} points at (dev, since there is no isolated test
 * profile/container here — see {@code @AutoConfigureTestDatabase(replace =
 * NONE)} above). {@link #cleanUp()} deletes everything this test commits,
 * in FK order (stocks before products — {@code stocks.variant_id} has no
 * cascade; products→variants/images do cascade at the DB level).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = "com.livecomerce.catalog.infrastructure.persistence")
@Import(CartProductInfoAdapter.class)
class CartProductInfoAdapterIntegrationTest {

    @Autowired LoadCartProductInfoPort loadCartProductInfoPort;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;

    private UUID seededUserId;
    private UUID seededStoreId;

    @TestConfiguration
    static class Stubs {
        @Bean
        LoadLiveProductStatusPort loadLiveProductStatusPort() {
            return productIds -> Map.of();
        }
    }

    @Test
    void loadForCart_afterSetupSessionCloses_doesNotThrowLazyInitializationException() {
        var storeId = seedStore();
        var product = Product.create(storeId, "Playera", "desc",
                new BigDecimal("199.00"), "MXN", "SKU-CART-IT-" + storeId, null);
        product.defaultVariant().addStock(10);
        product.addImage("https://example.com/img.jpg", 0, true);
        entityManager.persist(product);
        entityManager.flush();
        entityManager.clear();

        // Commit and close the setup session — loadForCart must open its own,
        // exactly like it does at runtime with no live HTTP request/session.
        TestTransaction.flagForCommit();
        TestTransaction.end();

        var ref = new CartLineRef(product.getId(), null);

        var result = loadCartProductInfoPort.loadForCart(List.of(ref));

        assertThat(result).containsKey(ref);
        var info = result.get(ref);
        assertThat(info.imageUrl()).isEqualTo("https://example.com/img.jpg");
        assertThat(info.availableStock()).isEqualTo(10);
    }

    /**
     * Undoes what {@link TestTransaction#end()} committed for real. Runs in
     * its own fresh (post-{@code TestTransaction.end()}) transaction — the
     * one {@code @DataJpaTest} would otherwise roll back never covers this
     * data, since it was committed and closed mid-test on purpose.
     */
    @AfterEach
    void cleanUp() {
        if (seededStoreId == null) return;

        jdbc.update("""
                DELETE FROM stocks WHERE variant_id IN (
                    SELECT id FROM product_variants WHERE product_id IN (
                        SELECT id FROM products WHERE store_id = ?
                    )
                )
                """, seededStoreId);
        // Deleting products cascades to product_variants and product_images
        // at the DB level (ON DELETE CASCADE) — see migration FKs.
        jdbc.update("DELETE FROM products WHERE store_id = ?", seededStoreId);
        jdbc.update("DELETE FROM stores WHERE id = ?", seededStoreId);
        jdbc.update("DELETE FROM users WHERE id = ?", seededUserId);
    }

    private UUID seedStore() {
        seededUserId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, role, first_name, last_name)
                VALUES (?, ?, 'hash', 'SELLER', 'Seller', 'Test')
                """, seededUserId, "seller-" + seededUserId + "@test.com");

        seededStoreId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO stores (id, user_id, name, slug)
                VALUES (?, ?, 'Test Store', ?)
                """, seededStoreId, seededUserId, "store-" + seededStoreId);
        return seededStoreId;
    }
}
