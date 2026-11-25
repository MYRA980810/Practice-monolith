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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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

    // ── findBySeller ─────────────────────────────────────────────────────────

    @Test
    void findBySeller_returnsOnlyThatSellersBans_newestFirst() {
        var oldUser   = UUID.randomUUID();
        var newUser   = UUID.randomUUID();
        insertBan(sellerId, oldUser, OffsetDateTime.parse("2026-11-20T10:00:00Z"));
        insertBan(sellerId, newUser, OffsetDateTime.parse("2026-11-22T10:00:00Z"));
        insertBan(otherSellerId, UUID.randomUUID(), OffsetDateTime.parse("2026-11-23T10:00:00Z"));

        var bans = adapter.findBySeller(sellerId, 1000);

        // Bans are per seller (not per live): every ban of the seller is listed, no other seller's.
        assertThat(bans).extracting(ChatBan::getUserId).containsExactly(newUser, oldUser);
        assertThat(bans).allMatch(b -> b.getSellerId().equals(sellerId));
    }

    @Test
    void findBySeller_sameCreatedAt_isOrderedDeterministicallyByIdDesc() {
        var at   = OffsetDateTime.parse("2026-11-22T10:00:00Z");
        var lowId  = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var highId = UUID.fromString("ffffffff-0000-0000-0000-000000000001");
        var lowUser  = UUID.randomUUID();
        var highUser = UUID.randomUUID();
        insertBan(lowId, sellerId, lowUser, at);
        insertBan(highId, sellerId, highUser, at);

        var bans = adapter.findBySeller(sellerId, 1000);

        assertThat(bans).extracting(ChatBan::getUserId).containsExactly(highUser, lowUser);
    }

    @Test
    void findBySeller_isCappedAtTheLimit_keepingTheNewest() {
        var oldest = UUID.randomUUID();
        var middle = UUID.randomUUID();
        var newest = UUID.randomUUID();
        insertBan(sellerId, oldest, OffsetDateTime.parse("2026-11-20T10:00:00Z"));
        insertBan(sellerId, middle, OffsetDateTime.parse("2026-11-21T10:00:00Z"));
        insertBan(sellerId, newest, OffsetDateTime.parse("2026-11-22T10:00:00Z"));

        assertThat(adapter.findBySeller(sellerId, 2)).extracting(ChatBan::getUserId).containsExactly(newest, middle);
    }

    @Test
    void findBySeller_noBans_returnsEmptyList() {
        assertThat(adapter.findBySeller(sellerId, 1000)).isEmpty();
    }

    @Test
    void reBan_keepsTheOriginalBanTime() {
        var original = ChatBan.create(sellerId, userId, "first", sellerId);
        var originalAt = OffsetDateTime.parse("2026-11-01T08:00:00Z");
        ReflectionTestUtils.setField(original, "createdAt", originalAt);
        adapter.banIfAbsent(original);

        adapter.banIfAbsent(ChatBan.create(sellerId, userId, "second", sellerId));

        var bans = adapter.findBySeller(sellerId, 1000);
        assertThat(bans).hasSize(1);
        assertThat(bans.getFirst().getCreatedAt().toInstant()).isEqualTo(originalAt.toInstant());
    }

    private void insertBan(UUID seller, UUID user, OffsetDateTime createdAt) {
        insertBan(UUID.randomUUID(), seller, user, createdAt);
    }

    private void insertBan(UUID id, UUID seller, UUID user, OffsetDateTime createdAt) {
        jdbc.update("""
                INSERT INTO chat_bans (id, seller_id, user_id, reason, banned_by, created_at)
                VALUES (?, ?, ?, NULL, ?, ?)
                """, id, seller, user, seller, createdAt);
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
