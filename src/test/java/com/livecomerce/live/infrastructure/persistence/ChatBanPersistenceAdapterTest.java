package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.domain.ChatBan;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against the real (Flyway-migrated) PostgreSQL schema so the UNIQUE(seller_id, user_id)
 * constraint is what makes {@code banIfAbsent} idempotent. {@code banIfAbsent} commits in its own
 * transaction (REQUIRES_NEW), so the test runs without a wrapping transaction and cleans up.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ChatBanPersistenceAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ChatBanPersistenceAdapterTest {

    @Autowired ChatBanPersistenceAdapter adapter;
    @Autowired JdbcTemplate              jdbc;

    private UUID sellerId;
    private UUID otherSellerId;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void seedSellers() {
        sellerId      = seedSeller();
        otherSellerId = seedSeller();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM chat_bans WHERE seller_id IN (?, ?)", sellerId, otherSellerId);
        jdbc.update("DELETE FROM users WHERE id IN (?, ?)", sellerId, otherSellerId);
    }

    @Test
    void banIfAbsent_firstBan_isStored() {
        adapter.banIfAbsent(ChatBan.create(sellerId, userId, "spam", sellerId));

        assertThat(adapter.isBanned(sellerId, userId)).isTrue();
        assertThat(jdbc.queryForObject("SELECT reason FROM chat_bans WHERE seller_id = ? AND user_id = ?",
                String.class, sellerId, userId)).isEqualTo("spam");
    }

    @Test
    void banIfAbsent_duplicateBan_isNoOp_andKeepsTheOriginalRow() {
        adapter.banIfAbsent(ChatBan.create(sellerId, userId, "first", sellerId));

        adapter.banIfAbsent(ChatBan.create(sellerId, userId, "second", sellerId));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chat_bans WHERE seller_id = ? AND user_id = ?",
                Integer.class, sellerId, userId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT reason FROM chat_bans WHERE seller_id = ? AND user_id = ?",
                String.class, sellerId, userId)).isEqualTo("first");
    }

    @Test
    void isBanned_isScopedToTheSeller() {
        adapter.banIfAbsent(ChatBan.create(sellerId, userId, null, sellerId));

        assertThat(adapter.isBanned(otherSellerId, userId)).isFalse();
        assertThat(adapter.isBanned(sellerId, UUID.randomUUID())).isFalse();
    }

    @Test
    void delete_removesOnlyThatSellersBan() {
        adapter.banIfAbsent(ChatBan.create(sellerId, userId, null, sellerId));
        adapter.banIfAbsent(ChatBan.create(otherSellerId, userId, null, otherSellerId));

        adapter.delete(sellerId, userId);

        assertThat(adapter.isBanned(sellerId, userId)).isFalse();
        assertThat(adapter.isBanned(otherSellerId, userId)).isTrue();
    }

    @Test
    void delete_whenNotBanned_isNoOp() {
        adapter.delete(sellerId, userId);

        assertThat(adapter.isBanned(sellerId, userId)).isFalse();
    }

    private UUID seedSeller() {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, role, first_name, last_name)
                VALUES (?, ?, 'hash', 'SELLER', 'Seller', 'Test')
                """, id, "seller-" + id + "@test.com");
        return id;
    }
}
