package com.livecomerce.live.infrastructure.ivs;

/**
 * An {@code ivs-chat-messages} SQS message that can never be persisted (malformed JSON, missing
 * field, invalid UUID or timestamp). Thrown, not swallowed, so the message lands in the DLQ.
 */
class InvalidChatMessagePayloadException extends RuntimeException {

    InvalidChatMessagePayloadException(String message) {
        super(message);
    }

    InvalidChatMessagePayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
