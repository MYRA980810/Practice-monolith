package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.domain.LiveChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface LiveChatMessageJpaRepository extends JpaRepository<LiveChatMessage, UUID> {

    boolean existsByProviderMsgId(String providerMsgId);

    /**
     * Race-safe idempotent insert: a redelivered message (same provider id) is skipped by the
     * database instead of raising a constraint violation, so the transaction is never marked
     * rollback-only. The provider id lives in the legacy {@code agora_msg_id} column (see
     * {@code LiveChatMessage#providerMsgId}).
     *
     * @return 1 when the message was inserted, 0 when it already existed
     */
    @Modifying
    @Query(value = """
            INSERT INTO live_chat_messages (id, live_id, agora_msg_id, user_id, username, content, sent_at)
            VALUES (:id, :liveId, :providerMsgId, :userId, :username, :content, :sentAt)
            ON CONFLICT (agora_msg_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("liveId") UUID liveId,
                       @Param("providerMsgId") String providerMsgId,
                       @Param("userId") UUID userId,
                       @Param("username") String username,
                       @Param("content") String content,
                       @Param("sentAt") OffsetDateTime sentAt);

    @Query("SELECT m FROM LiveChatMessage m WHERE m.live.id = :liveId AND m.sentAt < :before "
            + "AND m.deletedAt IS NULL ORDER BY m.sentAt DESC")
    List<LiveChatMessage> findByLiveIdAndSentAtBeforeOrderBySentAtDesc(
            @Param("liveId") UUID liveId,
            @Param("before") OffsetDateTime before,
            Pageable pageable);

    @Query("SELECT m FROM LiveChatMessage m WHERE m.live.id = :liveId AND m.deletedAt IS NULL ORDER BY m.sentAt DESC")
    List<LiveChatMessage> findByLiveIdOrderBySentAtDesc(
            @Param("liveId") UUID liveId,
            Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE LiveChatMessage m SET m.deletedAt = :deletedAt "
            + "WHERE m.live.id = :liveId AND m.providerMsgId = :msgId AND m.deletedAt IS NULL")
    int markDeleted(@Param("liveId") UUID liveId,
                    @Param("msgId") String msgId,
                    @Param("deletedAt") OffsetDateTime deletedAt);
}
