package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveChatMessage;
import com.livecomerce.live.domain.LiveContext;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chat message dedupe against the real (Flyway-migrated) PostgreSQL schema: a redelivered message
 * (same provider message id, column {@code agora_msg_id}) is skipped by {@code ON CONFLICT DO NOTHING}
 * without failing the surrounding transaction.
 * Default {@code @DataJpaTest} rollback leaves no committed rows.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LiveChatMessagePersistenceAdapter.class)
class LiveChatMessageDedupeTest {

    @Autowired LiveChatMessagePersistenceAdapter adapter;
    @Autowired EntityManager                     entityManager;
    @Autowired JdbcTemplate                      jdbc;

    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-11-20T10:00:00Z");

    private Live live;

    @BeforeEach
    void seed() {
        var sellerId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, role, first_name, last_name)
                VALUES (?, ?, 'hash', 'SELLER', 'Seller', 'Test')
                """, sellerId, "seller-" + sellerId + "@test.com");
        live = Live.create(sellerId, null, LiveContext.SELLER_PROFILE, "Live", null, null, 60);
        live.start();
        entityManager.persist(live);
        entityManager.flush();
    }

    @Test
    void saveIfAbsent_duplicateProviderMsgId_isSkippedWithoutError() {
        var msgId = "ivs-" + UUID.randomUUID();
        var first = LiveChatMessage.create(live, msgId, UUID.randomUUID(), "alice", "hi", T0);
        var redelivered = LiveChatMessage.create(live, msgId, first.getUserId(), "alice", "hi", T0);

        assertThat(adapter.saveIfAbsent(first)).isTrue();
        assertThat(adapter.saveIfAbsent(redelivered)).isFalse();

        assertThat(adapter.existsByProviderMsgId(msgId)).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM live_chat_messages WHERE agora_msg_id = ?", Integer.class, msgId))
                .isEqualTo(1);
        // The transaction is still usable after the skipped duplicate.
        assertThat(adapter.findByLiveIdLatest(live.getId(), 10))
                .extracting(LiveChatMessage::getProviderMsgId).containsExactly(msgId);
    }
}
