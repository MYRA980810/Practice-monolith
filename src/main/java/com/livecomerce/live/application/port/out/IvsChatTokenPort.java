package com.livecomerce.live.application.port.out;

import java.time.Instant;
import java.util.Map;

/** Issues chat tokens on the chat provider (Amazon IVS Chat). */
public interface IvsChatTokenPort {

    /**
     * Creates a token that lets {@code userId} connect to the room and send messages.
     * Moderation capabilities are never granted: moderation goes through the backend.
     *
     * @param attributes string attributes attached to every message the user sends (1 KB total limit)
     * @throws com.livecomerce.live.domain.ChatProviderUnavailableException if the provider fails
     */
    ChatToken createToken(String roomArn, String userId, Map<String, String> attributes,
                          int sessionDurationMinutes);

    record ChatToken(String token, Instant sessionExpirationTime, Instant tokenExpirationTime, String region) {}
}
