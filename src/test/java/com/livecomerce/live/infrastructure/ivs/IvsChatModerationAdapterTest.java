package com.livecomerce.live.infrastructure.ivs;

import com.livecomerce.live.domain.ChatProviderMisconfiguredException;
import com.livecomerce.live.domain.ChatProviderUnavailableException;
import com.livecomerce.live.domain.ChatRequestRejectedException;
import com.livecomerce.live.domain.ChatRoomNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ivschat.IvschatClient;
import software.amazon.awssdk.services.ivschat.model.AccessDeniedException;
import software.amazon.awssdk.services.ivschat.model.DeleteMessageRequest;
import software.amazon.awssdk.services.ivschat.model.DisconnectUserRequest;
import software.amazon.awssdk.services.ivschat.model.InternalServerException;
import software.amazon.awssdk.services.ivschat.model.PendingVerificationException;
import software.amazon.awssdk.services.ivschat.model.ResourceNotFoundException;
import software.amazon.awssdk.services.ivschat.model.ThrottlingException;
import software.amazon.awssdk.services.ivschat.model.ValidationException;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IvsChatModerationAdapterTest {

    @Mock IvschatClient ivschatClient;

    private static final String ROOM_ARN = "arn:aws:ivschat:us-east-1:123:room/abc";
    private static final String USER_ID  = "5f0c1b9e-0000-0000-0000-000000000001";

    private IvsChatModerationAdapter adapter() {
        return new IvsChatModerationAdapter(ivschatClient);
    }

    /** IVS error → domain exception, shared by both actions (except room-not-found, which differs). */
    static Stream<Arguments> errorMapping() {
        return Stream.of(
                Arguments.of(ThrottlingException.builder().statusCode(429).message("slow down").build(),
                        ChatProviderUnavailableException.class),
                Arguments.of(InternalServerException.builder().statusCode(500).message("boom").build(),
                        ChatProviderUnavailableException.class),
                Arguments.of(ApiCallTimeoutException.builder().message("timeout").build(),
                        ChatProviderUnavailableException.class),
                Arguments.of(SdkClientException.builder().message("network").build(),
                        ChatProviderUnavailableException.class),
                Arguments.of(ValidationException.builder().statusCode(400).message("bad id").build(),
                        ChatRequestRejectedException.class),
                Arguments.of(AccessDeniedException.builder().statusCode(403).message("no iam").build(),
                        ChatProviderMisconfiguredException.class),
                Arguments.of(PendingVerificationException.builder().statusCode(403).message("verify").build(),
                        ChatProviderMisconfiguredException.class));
    }

    // ── disconnectUser ───────────────────────────────────────────────────────

    @Test
    void disconnectUser_sendsRoomUserAndReason() {
        adapter().disconnectUser(ROOM_ARN, USER_ID, "spam");

        var captor = ArgumentCaptor.forClass(DisconnectUserRequest.class);
        verify(ivschatClient).disconnectUser(captor.capture());
        assertThat(captor.getValue().roomIdentifier()).isEqualTo(ROOM_ARN);
        assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().reason()).isEqualTo("spam");
    }

    @Test
    void disconnectUser_withoutReason_omitsIt() {
        adapter().disconnectUser(ROOM_ARN, USER_ID, null);

        var captor = ArgumentCaptor.forClass(DisconnectUserRequest.class);
        verify(ivschatClient).disconnectUser(captor.capture());
        assertThat(captor.getValue().reason()).isNull();
    }

    @Test
    void disconnectUser_roomGone_isNoOp() {
        when(ivschatClient.disconnectUser(any(DisconnectUserRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().statusCode(404).message("room not found").build());

        assertThatCode(() -> adapter().disconnectUser(ROOM_ARN, USER_ID, null)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("errorMapping")
    void disconnectUser_mapsProviderErrors_withoutLeakingArn(SdkException error, Class<?> expected) {
        when(ivschatClient.disconnectUser(any(DisconnectUserRequest.class))).thenThrow(error);

        assertThatThrownBy(() -> adapter().disconnectUser(ROOM_ARN, USER_ID, null))
                .isExactlyInstanceOf(expected)
                .message().doesNotContain(ROOM_ARN);
    }

    // ── deleteMessage ────────────────────────────────────────────────────────

    @Test
    void deleteMessage_sendsRoomAndId_withoutReason() {
        adapter().deleteMessage(ROOM_ARN, "msg-1");

        var captor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(ivschatClient).deleteMessage(captor.capture());
        assertThat(captor.getValue().roomIdentifier()).isEqualTo(ROOM_ARN);
        assertThat(captor.getValue().id()).isEqualTo("msg-1");
        assertThat(captor.getValue().reason()).isNull();
    }

    @Test
    void deleteMessage_roomGone_throwsRoomNotFound() {
        when(ivschatClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().statusCode(404).message("room not found").build());

        assertThatThrownBy(() -> adapter().deleteMessage(ROOM_ARN, "msg-1"))
                .isExactlyInstanceOf(ChatRoomNotFoundException.class)
                .message().doesNotContain(ROOM_ARN);
    }

    @ParameterizedTest
    @MethodSource("errorMapping")
    void deleteMessage_mapsProviderErrors_withoutLeakingArn(SdkException error, Class<?> expected) {
        when(ivschatClient.deleteMessage(any(DeleteMessageRequest.class))).thenThrow(error);

        assertThatThrownBy(() -> adapter().deleteMessage(ROOM_ARN, "msg-1"))
                .isExactlyInstanceOf(expected)
                .message().doesNotContain(ROOM_ARN);
    }
}
