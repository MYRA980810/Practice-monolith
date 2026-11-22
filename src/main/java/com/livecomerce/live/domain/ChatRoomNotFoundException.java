package com.livecomerce.live.domain;

/**
 * The chat room no longer exists on the provider (deleted when the live ended). Callers translate it
 * into {@link LiveChatRoomNotAvailableException}, which carries the live id.
 */
public class ChatRoomNotFoundException extends RuntimeException {

    public ChatRoomNotFoundException(Throwable cause) {
        super("Chat room not found", cause);
    }
}
