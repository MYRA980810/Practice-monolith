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

    boolean existsByAgoraMsgId(String agoraMsgId);

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
            + "WHERE m.live.id = :liveId AND m.agoraMsgId = :msgId AND m.deletedAt IS NULL")
    int markDeleted(@Param("liveId") UUID liveId,
                    @Param("msgId") String msgId,
                    @Param("deletedAt") OffsetDateTime deletedAt);
}
