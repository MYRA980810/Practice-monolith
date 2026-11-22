package com.livecomerce.live.application.port.out;

/**
 * Moderation actions on the chat provider (Amazon IVS Chat). Both methods throw
 * {@link com.livecomerce.live.domain.ChatProviderUnavailableException} on retryable failures,
 * {@link com.livecomerce.live.domain.ChatRequestRejectedException} when the provider rejects the input and
 * {@link com.livecomerce.live.domain.ChatProviderMisconfiguredException} when it refuses the call (IAM).
 */
public interface ChatModerationPort {

    /** Closes every open connection of {@code userId} to the room. A room that no longer exists is a no-op. */
    void disconnectUser(String roomArn, String userId, String reason);

    /**
     * Tells every client in the room to remove the message {@code messageId}.
     *
     * @throws com.livecomerce.live.domain.ChatRoomNotFoundException if the room no longer exists
     */
    void deleteMessage(String roomArn, String messageId);
}
