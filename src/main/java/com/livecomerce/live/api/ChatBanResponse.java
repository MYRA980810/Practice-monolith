package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase.BannedChatUser;

import java.time.Instant;
import java.util.UUID;

/** A banned chat user; {@code bannedAt} is an {@link Instant} so it always serializes as UTC ("Z"). */
public record ChatBanResponse(UUID userId, String displayName, Instant bannedAt) {

    static ChatBanResponse from(BannedChatUser ban) {
        return new ChatBanResponse(ban.userId(), ban.displayName(), ban.bannedAt());
    }
}
