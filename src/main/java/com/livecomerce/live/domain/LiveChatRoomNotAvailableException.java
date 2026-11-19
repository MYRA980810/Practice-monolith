package com.livecomerce.live.domain;

import java.util.UUID;

/** The live has no IVS Chat room (it was started while the chat provider was not IVS). */
public class LiveChatRoomNotAvailableException extends RuntimeException {

    public LiveChatRoomNotAvailableException(UUID liveId) {
        super("Chat room is not available for this live: " + liveId);
    }
}
