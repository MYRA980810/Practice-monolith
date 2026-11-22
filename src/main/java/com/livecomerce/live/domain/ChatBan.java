package com.livecomerce.live.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A user banned from the chat of every live of a seller (bans are per seller, not per live). */
@Entity
@Table(name = "chat_bans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatBan {

    @Id
    private UUID id;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private UUID sellerId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "reason", length = 256)
    private String reason;

    @Column(name = "banned_by", nullable = false, updatable = false)
    private UUID bannedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static ChatBan create(UUID sellerId, UUID userId, String reason, UUID bannedBy) {
        var ban = new ChatBan();
        ban.id        = UUID.randomUUID();
        ban.sellerId  = sellerId;
        ban.userId    = userId;
        ban.reason    = reason;
        ban.bannedBy  = bannedBy;
        ban.createdAt = OffsetDateTime.now();
        return ban;
    }
}
