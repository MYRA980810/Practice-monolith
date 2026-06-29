package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.domain.LiveChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface LiveChatMessageJpaRepository extends JpaRepository<LiveChatMessage, UUID> {

    boolean existsByAgoraMsgId(String agoraMsgId);

    @Query("SELECT m FROM LiveChatMessage m WHERE m.live.id = :liveId AND m.sentAt < :before ORDER BY m.sentAt DESC")
    List<LiveChatMessage> findByLiveIdAndSentAtBeforeOrderBySentAtDesc(
            @Param("liveId") UUID liveId,
            @Param("before") OffsetDateTime before,
            Pageable pageable);

    @Query("SELECT m FROM LiveChatMessage m WHERE m.live.id = :liveId ORDER BY m.sentAt DESC")
    List<LiveChatMessage> findByLiveIdOrderBySentAtDesc(
            @Param("liveId") UUID liveId,
            Pageable pageable);
}
