package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import com.livecomerce.live.domain.LiveStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Exercises the JPQL of {@link LiveJpaRepository#countByStatusGroupedByCategory}
 * against a real PostgreSQL instance (Flyway-migrated). The query is global (not scoped
 * to a seller or store), so pre-existing LIVE rows are ended inside the test transaction;
 * default {@code @DataJpaTest} rollback restores them and leaves no committed rows.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LiveJpaRepositoryCategoryCountTest {

    @Autowired LiveJpaRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    private UUID sellerId;

    @BeforeEach
    void isolateFromExistingLives() {
        jdbc.update("UPDATE lives SET status = 'ENDED' WHERE status = 'LIVE'");
        sellerId = seedSeller();
    }

    @Test
    void countsOnlyLiveStatus() {
        var categoryId = seedCategory();

        persist(categoryId, Live::start);
        persist(categoryId, live -> { });
        persist(categoryId, live -> { live.start(); live.beginReconnecting(); });
        persist(categoryId, live -> { live.start(); live.end(); });
        persist(categoryId, Live::cancel);
        flushAndClear();

        var rows = repository.countByStatusGroupedByCategory(LiveStatus.LIVE);

        assertThat(rows).extracting(r -> r[0], r -> ((Number) r[1]).longValue())
                .containsExactly(tuple(categoryId, 1L));
    }

    @Test
    void excludesLivesWithoutCategory() {
        persist(null, Live::start);
        flushAndClear();

        var rows = repository.countByStatusGroupedByCategory(LiveStatus.LIVE);

        assertThat(rows).isEmpty();
    }

    @Test
    void ordersByCountDescendingThenCategoryIdAscending() {
        var busiest = seedCategory();
        var tiedA = seedCategory();
        var tiedB = seedCategory();
        // PostgreSQL orders UUIDs by unsigned bytes, which matches their hex string order
        // (UUID.compareTo uses signed longs and can disagree).
        var lower = tiedA.toString().compareTo(tiedB.toString()) < 0 ? tiedA : tiedB;
        var higher = lower == tiedA ? tiedB : tiedA;

        persist(busiest, Live::start);
        persist(busiest, Live::start);
        persist(busiest, Live::start);
        persist(higher, Live::start);
        persist(lower, Live::start);
        flushAndClear();

        var rows = repository.countByStatusGroupedByCategory(LiveStatus.LIVE);

        assertThat(rows).extracting(r -> r[0], r -> ((Number) r[1]).longValue())
                .containsExactly(
                        tuple(busiest, 3L),
                        tuple(lower, 1L),
                        tuple(higher, 1L));
    }

    @Test
    void returnsEmptyWhenNoLiveLives() {
        var rows = repository.countByStatusGroupedByCategory(LiveStatus.LIVE);

        assertThat(rows).isEmpty();
    }

    @Test
    void countsMatchCategoryFilteredFeedTotals() {
        var cat1 = seedCategory();
        var cat2 = seedCategory();

        persist(cat1, Live::start);
        persist(cat1, Live::start);
        persist(cat1, live -> { live.start(); live.beginReconnecting(); });
        persist(cat2, Live::start);
        persist(cat2, live -> { });
        flushAndClear();

        var rows = repository.countByStatusGroupedByCategory(LiveStatus.LIVE);

        assertThat(rows).hasSize(2);
        for (var row : rows) {
            var categoryId = (UUID) row[0];
            var feedTotal = repository.findByStatusAndCategoryId(LiveStatus.LIVE, categoryId, PageRequest.of(0, 20))
                    .getTotalElements();
            assertThat(((Number) row[1]).longValue()).isEqualTo(feedTotal);
        }
    }

    private void persist(UUID categoryId, Consumer<Live> transition) {
        var live = Live.create(sellerId, null, LiveContext.SELLER_PROFILE, "Live", null, null, 60, categoryId);
        transition.accept(live);
        entityManager.persist(live);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private UUID seedSeller() {
        var userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, role, first_name, last_name)
                VALUES (?, ?, 'hash', 'SELLER', 'Seller', 'Test')
                """, userId, "seller-" + userId + "@test.com");
        return userId;
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
