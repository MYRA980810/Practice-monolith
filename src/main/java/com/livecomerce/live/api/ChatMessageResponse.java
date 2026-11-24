package com.livecomerce.live.api;

import com.livecomerce.live.domain.LiveChatMessage;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One chat history entry.
 *
 * @param id                internal id of the stored message (kept for backward compatibility)
 * @param providerMessageId chat provider's message id (IVS Chat / Agora RTM): the id the seller passes to
 *                          {@code DELETE /api/lives/{liveId}/chat/messages/{messageId}} to delete it
 */
public record ChatMessageResponse(UUID id, String providerMessageId, UUID userId, String username,
                                  String content, OffsetDateTime sentAt) {

    public static ChatMessageResponse from(LiveChatMessage msg) {
        return new ChatMessageResponse(
                msg.getId(),
                msg.getProviderMsgId(),
                msg.getUserId(),
                msg.getUsername(),
                msg.getContent(),
                msg.getSentAt()
        );
    }
}
