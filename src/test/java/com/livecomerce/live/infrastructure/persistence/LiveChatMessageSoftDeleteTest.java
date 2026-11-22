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
 * Soft delete of chat messages against the real (Flyway-migrated) PostgreSQL schema:
 * marking is scoped to the live, and the history queries skip deleted messages.
 * Default {@code @DataJpaTest} rollback leaves no committed rows.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LiveChatMessagePersistenceAdapter.class)
class LiveChatMessageSoftDeleteTest {

    @Autowired LiveChatMessagePersistenceAdapter adapter;
    @Autowired EntityManager                     entityManager;
    @Autowired JdbcTemplate                      jdbc;

    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-11-20T10:00:00Z");

    private Live live;
    private Live otherLive;

    @BeforeEach
    void seed() {
        var sellerId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, role, first_name, last_name)
                VALUES (?, ?, 'hash', 'SELLER', 'Seller', 'Test')
                """, sellerId, "seller-" + sellerId + "@test.com");
        live      = persistLive(sellerId);
        otherLive = persistLive(sellerId);
    }

    @Test
    void markDeleted_marksOnlyTheMessageOfThatLive() {
        var msgId = "ivs-" + UUID.randomUUID();
        persistMessage(live, msgId, T0);

        var marked = adapter.markDeleted(otherLive.getId(), msgId, T0.plusMinutes(1));

        assertThat(marked).isZero();
        assertThat(deletedAt(msgId)).isNull();

        assertThat(adapter.markDeleted(live.getId(), msgId, T0.plusMinutes(1))).isEqualTo(1);
        assertThat(deletedAt(msgId)).isNotNull();
    }

    @Test
    void markDeleted_unknownMessage_marksNothing() {
        assertThat(adapter.markDeleted(live.getId(), "missing-" + UUID.randomUUID(), T0)).isZero();
    }

    @Test
    void history_excludesDeletedMessages() {
        var kept    = "ivs-" + UUID.randomUUID();
        var deleted = "ivs-" + UUID.randomUUID();
        persistMessage(live, kept, T0);
        persistMessage(live, deleted, T0.plusSeconds(1));
        adapter.markDeleted(live.getId(), deleted, T0.plusMinutes(1));
        entityManager.clear();

        assertThat(adapter.findByLiveIdLatest(live.getId(), 50))
                .extracting(LiveChatMessage::getProviderMsgId).containsExactly(kept);
        assertThat(adapter.findByLiveIdBefore(live.getId(), T0.plusMinutes(5), 50))
                .extracting(LiveChatMessage::getProviderMsgId).containsExactly(kept);
    }

    private Live persistLive(UUID sellerId) {
        var l = Live.create(sellerId, null, LiveContext.SELLER_PROFILE, "Live", null, null, 60);
        l.start();
        entityManager.persist(l);
        return l;
    }

    private void persistMessage(Live owner, String msgId, OffsetDateTime sentAt) {
        entityManager.persist(LiveChatMessage.create(owner, msgId, UUID.randomUUID(), "alice", "hi", sentAt));
        entityManager.flush();
    }

    private OffsetDateTime deletedAt(String msgId) {
        return jdbc.queryForObject("SELECT deleted_at FROM live_chat_messages WHERE agora_msg_id = ?",
                OffsetDateTime.class, msgId);
    }
}
