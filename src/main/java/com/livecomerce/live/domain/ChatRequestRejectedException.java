package com.livecomerce.live.domain;

/**
 * The chat provider rejected the request as invalid (IVS ValidationException): not retryable.
 * The message is generic on purpose, it is returned to the client.
 */
public class ChatRequestRejectedException extends RuntimeException {

    public ChatRequestRejectedException(Throwable cause) {
        super("The chat provider rejected the request", cause);
    }
}
