package com.livecomerce.live.domain;

public class ChatProviderUnavailableException extends RuntimeException {

    public static final String CODE = "CHAT_PROVIDER_UNAVAILABLE";

    public ChatProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
