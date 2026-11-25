package com.livecomerce.live.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Chat moderation by the seller who owns the live. */
public interface ModerateLiveChatUseCase {

    void banUser(BanChatUserCommand command);

    void unbanUser(UnbanChatUserCommand command);

    void deleteMessage(DeleteChatMessageCommand command);

    /**
     * Every active chat ban of the live's seller (bans are per seller; the live only resolves the
     * seller), newest first, capped. Never null; empty when the seller banned nobody.
     */
    List<BannedChatUser> listBans(ListChatBansQuery query);

    record ListChatBansQuery(UUID liveId, UUID sellerId) {}

    /** {@code bannedAt} is the time of the original ban (a re-ban does not refresh it). */
    record BannedChatUser(UUID userId, String displayName, Instant bannedAt) {}

    record BanChatUserCommand(UUID liveId, UUID sellerId, UUID userId, String reason) {}

    record UnbanChatUserCommand(UUID liveId, UUID sellerId, UUID userId) {}

    /** {@code messageId} is the chat provider's message id. */
    record DeleteChatMessageCommand(UUID liveId, UUID sellerId, String messageId) {}
}
