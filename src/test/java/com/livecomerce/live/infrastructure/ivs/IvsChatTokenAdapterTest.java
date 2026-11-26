package com.livecomerce.live.infrastructure.ivs;

import com.livecomerce.live.domain.ChatProviderUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.services.ivschat.IvschatClient;
import software.amazon.awssdk.services.ivschat.model.ChatTokenCapability;
import software.amazon.awssdk.services.ivschat.model.CreateChatTokenRequest;
import software.amazon.awssdk.services.ivschat.model.CreateChatTokenResponse;
import software.amazon.awssdk.services.ivschat.model.IvschatException;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IvsChatTokenAdapterTest {

    @Mock IvschatClient ivschatClient;

    private static final String  ROOM_ARN       = "arn:aws:ivschat:us-east-1:123:room/abc";
    private static final String  USER_ID        = "5f0c1b9e-0000-0000-0000-000000000001";
    private static final Instant SESSION_EXPIRY = Instant.parse("2026-11-18T11:00:00Z");
    private static final Instant TOKEN_EXPIRY   = Instant.parse("2026-11-18T10:05:00Z");
    private static final Map<String, String> ATTRIBUTES =
            Map.of("liveId", "live-1", "displayName", "Ana", "role", "BUYER");

    private IvsChatTokenAdapter adapter() {
        return new IvsChatTokenAdapter(ivschatClient, "us-east-1");
    }

    @Test
    void createToken_buildsRequestAndMapsResponse() {
        when(ivschatClient.createChatToken(any(CreateChatTokenRequest.class)))
                .thenReturn(CreateChatTokenResponse.builder()
                        .token("ivs-token")
                        .sessionExpirationTime(SESSION_EXPIRY)
                        .tokenExpirationTime(TOKEN_EXPIRY)
                        .build());

        var token = adapter().createToken(ROOM_ARN, USER_ID, ATTRIBUTES, true, 60);

        assertThat(token.token()).isEqualTo("ivs-token");
        assertThat(token.sessionExpirationTime()).isEqualTo(SESSION_EXPIRY);
        assertThat(token.tokenExpirationTime()).isEqualTo(TOKEN_EXPIRY);
        assertThat(token.region()).isEqualTo("us-east-1");

        var captor = ArgumentCaptor.forClass(CreateChatTokenRequest.class);
        verify(ivschatClient).createChatToken(captor.capture());
        var request = captor.getValue();
        assertThat(request.roomIdentifier()).isEqualTo(ROOM_ARN);
        assertThat(request.userId()).isEqualTo(USER_ID);
        assertThat(request.capabilities()).containsExactly(ChatTokenCapability.SEND_MESSAGE);
        assertThat(request.attributes()).containsExactlyInAnyOrderEntriesOf(ATTRIBUTES);
        assertThat(request.sessionDurationInMinutes()).isEqualTo(60);
    }

    @Test
    void createToken_cannotSend_grantsNoCapabilities_soTheTokenIsReceiveOnly() {
        when(ivschatClient.createChatToken(any(CreateChatTokenRequest.class)))
                .thenReturn(CreateChatTokenResponse.builder()
                        .token("ivs-token")
                        .sessionExpirationTime(SESSION_EXPIRY)
                        .tokenExpirationTime(TOKEN_EXPIRY)
                        .build());

        var token = adapter().createToken(ROOM_ARN, USER_ID, ATTRIBUTES, false, 60);

        assertThat(token.token()).isEqualTo("ivs-token");
        var captor = ArgumentCaptor.forClass(CreateChatTokenRequest.class);
        verify(ivschatClient).createChatToken(captor.capture());
        var request = captor.getValue();
        assertThat(request.capabilities()).isEmpty();
        assertThat(request.roomIdentifier()).isEqualTo(ROOM_ARN);
        assertThat(request.userId()).isEqualTo(USER_ID);
        assertThat(request.attributes()).containsExactlyInAnyOrderEntriesOf(ATTRIBUTES);
        assertThat(request.sessionDurationInMinutes()).isEqualTo(60);
    }

    @Test
    void createToken_sdkFailure_throwsChatProviderUnavailable() {
        var sdkError = IvschatException.builder().message("throttled").build();
        when(ivschatClient.createChatToken(any(CreateChatTokenRequest.class))).thenThrow(sdkError);

        assertThatThrownBy(() -> adapter().createToken(ROOM_ARN, USER_ID, ATTRIBUTES, true, 60))
                .isInstanceOf(ChatProviderUnavailableException.class)
                .hasCause(sdkError);
    }

    @Test
    void createToken_sdkFailure_doesNotExposeRoomArnInMessage() {
        when(ivschatClient.createChatToken(any(CreateChatTokenRequest.class)))
                .thenThrow(IvschatException.builder().message("throttled").build());

        assertThatThrownBy(() -> adapter().createToken(ROOM_ARN, USER_ID, ATTRIBUTES, true, 60))
                .isInstanceOf(ChatProviderUnavailableException.class)
                .message().doesNotContain(ROOM_ARN).doesNotContain("arn:");
    }

    @Test
    void createToken_apiCallTimeout_throwsChatProviderUnavailable() {
        var timeout = ApiCallTimeoutException.create(3000);
        when(ivschatClient.createChatToken(any(CreateChatTokenRequest.class))).thenThrow(timeout);

        assertThatThrownBy(() -> adapter().createToken(ROOM_ARN, USER_ID, ATTRIBUTES, true, 60))
                .isInstanceOf(ChatProviderUnavailableException.class)
                .hasCause(timeout);
    }

    @Test
    void createToken_apiCallAttemptTimeout_throwsChatProviderUnavailable() {
        var timeout = ApiCallAttemptTimeoutException.create(1500);
        when(ivschatClient.createChatToken(any(CreateChatTokenRequest.class))).thenThrow(timeout);

        assertThatThrownBy(() -> adapter().createToken(ROOM_ARN, USER_ID, ATTRIBUTES, true, 60))
                .isInstanceOf(ChatProviderUnavailableException.class)
                .hasCause(timeout);
    }
}
