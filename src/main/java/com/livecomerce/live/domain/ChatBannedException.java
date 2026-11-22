package com.livecomerce.live.domain;

import java.util.UUID;

/** The user is banned from the chat of the live's seller. */
public class ChatBannedException extends RuntimeException {

    public static final String CODE = "CHAT_BANNED";

    public ChatBannedException(UUID liveId) {
        super("You are banned from the chat of this live: " + liveId);
    }
}
