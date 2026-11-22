package com.livecomerce.live.application.port.in;

import java.util.UUID;

/** Chat moderation by the seller who owns the live. */
public interface ModerateLiveChatUseCase {

    void banUser(BanChatUserCommand command);

    void unbanUser(UnbanChatUserCommand command);

    void deleteMessage(DeleteChatMessageCommand command);

    record BanChatUserCommand(UUID liveId, UUID sellerId, UUID userId, String reason) {}

    record UnbanChatUserCommand(UUID liveId, UUID sellerId, UUID userId) {}

    /** {@code messageId} is the chat provider's message id. */
    record DeleteChatMessageCommand(UUID liveId, UUID sellerId, String messageId) {}
}
