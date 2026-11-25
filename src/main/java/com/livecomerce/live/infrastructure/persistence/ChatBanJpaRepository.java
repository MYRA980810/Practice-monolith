package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.domain.ChatBan;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface ChatBanJpaRepository extends JpaRepository<ChatBan, UUID> {

    boolean existsBySellerIdAndUserId(UUID sellerId, UUID userId);

    /** Newest first; the id tiebreaker keeps the order deterministic for equal timestamps. */
    List<ChatBan> findBySellerIdOrderByCreatedAtDescIdDesc(UUID sellerId, Limit limit);

    /**
     * Race-safe idempotent insert: a duplicate (seller, user) is skipped by the database instead
     * of raising a constraint violation, so the transaction is never marked rollback-only.
     *
     * @return 1 when the ban was inserted, 0 when it already existed
     */
    @Modifying
    @Query(value = """
            INSERT INTO chat_bans (id, seller_id, user_id, reason, banned_by, created_at)
            VALUES (:id, :sellerId, :userId, :reason, :bannedBy, :createdAt)
            ON CONFLICT (seller_id, user_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("sellerId") UUID sellerId,
                       @Param("userId") UUID userId,
                       @Param("reason") String reason,
                       @Param("bannedBy") UUID bannedBy,
                       @Param("createdAt") OffsetDateTime createdAt);

    @Modifying
    @Query("DELETE FROM ChatBan b WHERE b.sellerId = :sellerId AND b.userId = :userId")
    int deleteBySellerIdAndUserId(@Param("sellerId") UUID sellerId, @Param("userId") UUID userId);
}
