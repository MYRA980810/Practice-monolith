package com.livecomerce.live.domain;

/** A seller tried to ban themself from their own chat. */
public class ChatSelfBanException extends RuntimeException {

    public static final String CODE = "CHAT_SELF_BAN";

    public ChatSelfBanException() {
        super("A seller cannot ban themself from their own chat");
    }
}
