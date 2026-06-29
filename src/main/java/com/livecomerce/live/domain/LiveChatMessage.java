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

    @Column(name = "agora_msg_id", nullable = false, length = 255)
    private String agoraMsgId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    public static LiveChatMessage create(Live live, String agoraMsgId, UUID userId,
                                         String username, String content, OffsetDateTime sentAt) {
        var msg      = new LiveChatMessage();
        msg.id       = UUID.randomUUID();
        msg.live     = live;
        msg.agoraMsgId = agoraMsgId;
        msg.userId   = userId;
        msg.username = username;
        msg.content  = content;
        msg.sentAt   = sentAt;
        return msg;
    }
}
