package com.livecomerce.live.application.port.out;

import java.util.UUID;

/** Per-live chat room lifecycle on the chat provider (Amazon IVS Chat). */
public interface ChatRoomPort {

    /**
     * Creates the chat room of a live and returns its ARN.
     *
     * @throws com.livecomerce.live.domain.ChatProviderUnavailableException if the provider fails
     */
    String createRoom(UUID liveId, UUID sellerId);

    /** Deletes a chat room; deleting a room that no longer exists is a no-op. */
    void deleteRoom(String roomArn);
}
