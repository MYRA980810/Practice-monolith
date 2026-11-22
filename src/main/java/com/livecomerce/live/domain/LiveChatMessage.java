package com.livecomerce.live.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LiveChatMessage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_id", nullable = false)
    private Live live;

    /**
     * Chat provider's message id (Agora RTM or IVS Chat); unique, used to dedupe redeliveries.
     * Still stored in the legacy {@code agora_msg_id} column: the physical rename to
     * {@code provider_msg_id} is deferred to the Agora cleanup PR (PR 6), so this deploy needs no
     * migration and old instances keep working during the rolling deploy.
     */
    @Column(name = "agora_msg_id", nullable = false, length = 255)
    private String providerMsgId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    /** Set when the seller deleted the message; deleted messages are excluded from the history. */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public static LiveChatMessage create(Live live, String providerMsgId, UUID userId,
                                         String username, String content, OffsetDateTime sentAt) {
        var msg      = new LiveChatMessage();
        msg.id       = UUID.randomUUID();
        msg.live     = live;
        msg.providerMsgId = providerMsgId;
        msg.userId   = userId;
        msg.username = username;
        msg.content  = content;
        msg.sentAt   = sentAt;
        return msg;
    }
}
