package com.livecomerce.live.api;

import com.livecomerce.live.domain.LiveChatMessage;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatMessageResponse(UUID id, UUID userId, String username, String content, OffsetDateTime sentAt) {

    public static ChatMessageResponse from(LiveChatMessage msg) {
        return new ChatMessageResponse(
                msg.getId(),
                msg.getUserId(),
                msg.getUsername(),
                msg.getContent(),
                msg.getSentAt()
        );
    }
}
