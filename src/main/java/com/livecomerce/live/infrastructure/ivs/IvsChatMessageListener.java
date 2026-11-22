package com.livecomerce.live.infrastructure.ivs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.in.SaveChatMessageUseCase;
import com.livecomerce.live.application.port.in.SaveChatMessageUseCase.SaveChatMessageCommand;
import com.livecomerce.live.infrastructure.ChatProviderConditions;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Persists the IVS Chat history: the message review Lambda sends every allowed message to the
 * {@code ivs-chat-messages} SQS queue and this listener saves it through {@link SaveChatMessageUseCase}.
 *
 * <p>Registered only when {@code live.chat-provider=ivs}: with {@code queue-not-found-strategy: fail}
 * an always-on listener would break startup wherever the queue is not provisioned.
 *
 * <p>Error semantics (acknowledgement is on success only):
 * <ul>
 *   <li>Duplicates (same {@code messageId}) are skipped by the use case and acknowledged.</li>
 *   <li>Every failure propagates, so the message returns to the queue and, after the queue's
 *       {@code maxReceiveCount}, lands in the DLQ. That includes poison messages (malformed JSON,
 *       missing field, invalid UUID/timestamp, unknown live): they are never silently dropped, so a
 *       Lambda/backend contract bug shows up as DLQ depth and the messages can be redriven after the fix.</li>
 * </ul>
 * Message content is never logged; only ids.
 */
@Component
@Conditional(ChatProviderConditions.IvsSelected.class)
class IvsChatMessageListener {

    private static final Logger log = LoggerFactory.getLogger(IvsChatMessageListener.class);

    private final SaveChatMessageUseCase saveChatMessageUseCase;
    private final ObjectMapper           objectMapper;

    IvsChatMessageListener(SaveChatMessageUseCase saveChatMessageUseCase, ObjectMapper objectMapper) {
        this.saveChatMessageUseCase = saveChatMessageUseCase;
        this.objectMapper           = objectMapper;
    }

    @SqsListener("${ivs.chat.messages-queue-name:ivs-chat-messages}")
    void onChatMessage(String rawMessage) {
        var payload = parse(rawMessage);
        try {
            saveChatMessageUseCase.saveChatMessage(toCommand(payload));
        } catch (RuntimeException e) {
            log.warn("Failed to persist IVS chat message, it will be redelivered: messageId={}, liveId={}, error={}",
                    payload.messageId(), payload.liveId(), e.toString());
            throw e;
        }
    }

    private IvsChatMessagePayload parse(String rawMessage) {
        try {
            return objectMapper.readValue(rawMessage, IvsChatMessagePayload.class);
        } catch (JsonProcessingException e) {
            log.warn("Unparseable IVS chat message, it will end up in the DLQ: {}", e.getOriginalMessage());
            throw new InvalidChatMessagePayloadException("Malformed IVS chat message JSON", e);
        }
    }

    private static SaveChatMessageCommand toCommand(IvsChatMessagePayload payload) {
        var messageId = required(payload.messageId(), "messageId");
        var userIdRaw = required(payload.userId(), "userId");
        var username  = payload.displayName() == null || payload.displayName().isBlank()
                ? userIdRaw : payload.displayName();
        return new SaveChatMessageCommand(
                uuid(payload.liveId(), "liveId"),
                messageId,
                uuid(userIdRaw, "userId"),
                username,
                required(payload.content(), "content"),
                timestamp(payload.sentAt()));
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidChatMessagePayloadException("IVS chat message without " + field);
        }
        return value;
    }

    private static UUID uuid(String value, String field) {
        var raw = required(value, field);
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new InvalidChatMessagePayloadException("IVS chat message with invalid " + field, e);
        }
    }

    private static OffsetDateTime timestamp(String value) {
        var raw = required(value, "sentAt");
        try {
            return OffsetDateTime.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidChatMessagePayloadException("IVS chat message with invalid sentAt", e);
        }
    }
}
