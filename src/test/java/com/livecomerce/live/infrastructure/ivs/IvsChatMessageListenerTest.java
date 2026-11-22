package com.livecomerce.live.infrastructure.ivs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.in.SaveChatMessageUseCase;
import com.livecomerce.live.application.port.in.SaveChatMessageUseCase.SaveChatMessageCommand;
import com.livecomerce.live.domain.LiveNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class IvsChatMessageListenerTest {

    @Mock SaveChatMessageUseCase saveChatMessageUseCase;

    private static final UUID   LIVE_ID    = UUID.randomUUID();
    private static final UUID   USER_ID    = UUID.randomUUID();
    private static final String MESSAGE_ID = "Ab1cDeFgHiJk";
    private static final String SENT_AT    = "2026-11-22T15:04:05.123Z";

    private IvsChatMessageListener listener() {
        return new IvsChatMessageListener(saveChatMessageUseCase, new ObjectMapper());
    }

    private static String payload(String messageId, String liveId, String userId,
                                  String displayName, String content, String sentAt) {
        return """
                {"messageId":%s,"liveId":%s,"userId":%s,"displayName":%s,"content":%s,"sentAt":%s,"extra":"ignored"}
                """.formatted(json(messageId), json(liveId), json(userId), json(displayName), json(content), json(sentAt));
    }

    private static String json(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static String valid() {
        return payload(MESSAGE_ID, LIVE_ID.toString(), USER_ID.toString(), "Alice", "Hello!", SENT_AT);
    }

    @Test
    void validMessage_savesMappedCommand() {
        listener().onChatMessage(valid());

        var captor = ArgumentCaptor.forClass(SaveChatMessageCommand.class);
        verify(saveChatMessageUseCase).saveChatMessage(captor.capture());
        var command = captor.getValue();
        assertThat(command.liveId()).isEqualTo(LIVE_ID);
        assertThat(command.providerMsgId()).isEqualTo(MESSAGE_ID);
        assertThat(command.userId()).isEqualTo(USER_ID);
        assertThat(command.username()).isEqualTo("Alice");
        assertThat(command.content()).isEqualTo("Hello!");
        assertThat(command.sentAt()).isEqualTo(OffsetDateTime.parse(SENT_AT));
    }

    @Test
    void missingDisplayName_fallsBackToUserId() {
        listener().onChatMessage(payload(MESSAGE_ID, LIVE_ID.toString(), USER_ID.toString(), null, "Hi", SENT_AT));

        var captor = ArgumentCaptor.forClass(SaveChatMessageCommand.class);
        verify(saveChatMessageUseCase).saveChatMessage(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo(USER_ID.toString());
    }

    @Test
    void duplicateDelivery_isAcknowledgedWithoutError() {
        // The use case dedupes by provider message id and returns normally, so the message is acked.
        var listener = listener();

        assertThatCode(() -> {
            listener.onChatMessage(valid());
            listener.onChatMessage(valid());
        }).doesNotThrowAnyException();
        verify(saveChatMessageUseCase, times(2)).saveChatMessage(any());
    }

    @Test
    void persistenceFailure_propagatesSoTheMessageIsRedelivered() {
        doThrow(new DataAccessResourceFailureException("db down"))
                .when(saveChatMessageUseCase).saveChatMessage(any());

        assertThatThrownBy(() -> listener().onChatMessage(valid()))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void unknownLive_propagatesToEndInTheDlq() {
        doThrow(new LiveNotFoundException(LIVE_ID)).when(saveChatMessageUseCase).saveChatMessage(any());

        assertThatThrownBy(() -> listener().onChatMessage(valid()))
                .isInstanceOf(LiveNotFoundException.class);
    }

    @Test
    void malformedJson_propagatesWithoutCallingTheUseCase() {
        assertThatThrownBy(() -> listener().onChatMessage("{not json"))
                .isInstanceOf(InvalidChatMessagePayloadException.class);
        verifyNoInteractions(saveChatMessageUseCase);
    }

    @Test
    void invalidLiveId_propagatesWithoutCallingTheUseCase() {
        assertThatThrownBy(() -> listener().onChatMessage(
                payload(MESSAGE_ID, "not-a-uuid", USER_ID.toString(), "Alice", "Hi", SENT_AT)))
                .isInstanceOf(InvalidChatMessagePayloadException.class);
        verifyNoInteractions(saveChatMessageUseCase);
    }

    @Test
    void invalidUserId_propagatesWithoutCallingTheUseCase() {
        assertThatThrownBy(() -> listener().onChatMessage(
                payload(MESSAGE_ID, LIVE_ID.toString(), "nope", "Alice", "Hi", SENT_AT)))
                .isInstanceOf(InvalidChatMessagePayloadException.class);
        verifyNoInteractions(saveChatMessageUseCase);
    }

    @Test
    void invalidSentAt_propagatesWithoutCallingTheUseCase() {
        assertThatThrownBy(() -> listener().onChatMessage(
                payload(MESSAGE_ID, LIVE_ID.toString(), USER_ID.toString(), "Alice", "Hi", "yesterday")))
                .isInstanceOf(InvalidChatMessagePayloadException.class);
        verifyNoInteractions(saveChatMessageUseCase);
    }

    @Test
    void missingRequiredFields_propagateWithoutCallingTheUseCase() {
        var listener = listener();
        var live = LIVE_ID.toString();
        var user = USER_ID.toString();

        for (var bad : new String[] {
                payload(null, live, user, "Alice", "Hi", SENT_AT),
                payload(" ", live, user, "Alice", "Hi", SENT_AT),
                payload(MESSAGE_ID, null, user, "Alice", "Hi", SENT_AT),
                payload(MESSAGE_ID, live, null, "Alice", "Hi", SENT_AT),
                payload(MESSAGE_ID, live, user, "Alice", null, SENT_AT),
                payload(MESSAGE_ID, live, user, "Alice", "Hi", null)}) {
            assertThatThrownBy(() -> listener.onChatMessage(bad))
                    .as(bad)
                    .isInstanceOf(InvalidChatMessagePayloadException.class);
        }
        verifyNoInteractions(saveChatMessageUseCase);
    }
}
