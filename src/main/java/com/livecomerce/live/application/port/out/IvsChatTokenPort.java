package com.livecomerce.live.application.port.out;

import java.time.Instant;
import java.util.Map;

/** Issues chat tokens on the chat provider (Amazon IVS Chat). */
public interface IvsChatTokenPort {

    /**
     * Creates a token that lets {@code userId} connect to the room and receive messages and events.
     * Moderation capabilities are never granted: moderation goes through the backend.
     *
     * @param attributes string attributes attached to every message the user sends (1 KB total limit)
     * @param canSend    {@code true} grants sending messages; {@code false} issues a receive-only token
     * @throws com.livecomerce.live.domain.ChatProviderUnavailableException if the provider fails
     */
    ChatToken createToken(String roomArn, String userId, Map<String, String> attributes,
                          boolean canSend, int sessionDurationMinutes);

    record ChatToken(String token, Instant sessionExpirationTime, Instant tokenExpirationTime, String region) {}
}
