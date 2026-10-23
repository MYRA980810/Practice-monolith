package com.livecomerce.live.infrastructure.ivs;

import com.livecomerce.live.domain.ChatProviderUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ivschat.IvschatClient;
import software.amazon.awssdk.services.ivschat.model.CreateRoomRequest;
import software.amazon.awssdk.services.ivschat.model.CreateRoomResponse;
import software.amazon.awssdk.services.ivschat.model.DeleteRoomRequest;
import software.amazon.awssdk.services.ivschat.model.FallbackResult;
import software.amazon.awssdk.services.ivschat.model.IvschatException;
import software.amazon.awssdk.services.ivschat.model.ResourceNotFoundException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IvsChatRoomAdapterTest {

    @Mock IvschatClient ivschatClient;

    private static final UUID   LIVE_ID     = UUID.randomUUID();
    private static final UUID   SELLER_ID   = UUID.randomUUID();
    private static final String ROOM_ARN    = "arn:aws:ivschat:us-east-1:123:room/abc";
    private static final String HANDLER_ARN = "arn:aws:lambda:us-east-1:123:function:chat-review";

    private IvsChatRoomAdapter adapter(String reviewHandlerArn) {
        return new IvsChatRoomAdapter(ivschatClient, 500, 10, reviewHandlerArn, "ALLOW");
    }

    private CreateRoomRequest captureCreateRequest() {
        var captor = ArgumentCaptor.forClass(CreateRoomRequest.class);
        verify(ivschatClient).createRoom(captor.capture());
        return captor.getValue();
    }

    @Test
    void createRoom_buildsRequestWithNameLimitsAndTags_andReturnsArn() {
        when(ivschatClient.createRoom(any(CreateRoomRequest.class)))
                .thenReturn(CreateRoomResponse.builder().arn(ROOM_ARN).build());

        var arn = adapter("").createRoom(LIVE_ID, SELLER_ID);

        assertThat(arn).isEqualTo(ROOM_ARN);
        var request = captureCreateRequest();
        assertThat(request.name()).isEqualTo("live-" + LIVE_ID);
        assertThat(request.maximumMessageLength()).isEqualTo(500);
        assertThat(request.maximumMessageRatePerSecond()).isEqualTo(10);
        assertThat(request.tags()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "liveId",   LIVE_ID.toString(),
                "sellerId", SELLER_ID.toString()));
    }

    @Test
    void createRoom_withoutReviewHandlerArn_omitsMessageReviewHandler() {
        when(ivschatClient.createRoom(any(CreateRoomRequest.class)))
                .thenReturn(CreateRoomResponse.builder().arn(ROOM_ARN).build());

        adapter("  ").createRoom(LIVE_ID, SELLER_ID);

        assertThat(captureCreateRequest().messageReviewHandler()).isNull();
    }

    @Test
    void createRoom_withReviewHandlerArn_setsHandlerUriAndFallbackResult() {
        when(ivschatClient.createRoom(any(CreateRoomRequest.class)))
                .thenReturn(CreateRoomResponse.builder().arn(ROOM_ARN).build());

        adapter(HANDLER_ARN).createRoom(LIVE_ID, SELLER_ID);

        var handler = captureCreateRequest().messageReviewHandler();
        assertThat(handler.uri()).isEqualTo(HANDLER_ARN);
        assertThat(handler.fallbackResult()).isEqualTo(FallbackResult.ALLOW);
    }

    @Test
    void createRoom_whenSdkFails_throwsChatProviderUnavailable() {
        when(ivschatClient.createRoom(any(CreateRoomRequest.class)))
                .thenThrow(IvschatException.builder().message("throttled").build());

        assertThatThrownBy(() -> adapter("").createRoom(LIVE_ID, SELLER_ID))
                .isInstanceOf(ChatProviderUnavailableException.class);
    }

    @Test
    void deleteRoom_deletesByArn() {
        adapter("").deleteRoom(ROOM_ARN);

        var captor = ArgumentCaptor.forClass(DeleteRoomRequest.class);
        verify(ivschatClient).deleteRoom(captor.capture());
        assertThat(captor.getValue().identifier()).isEqualTo(ROOM_ARN);
    }

    @Test
    void deleteRoom_whenRoomAlreadyGone_isNoOp() {
        when(ivschatClient.deleteRoom(any(DeleteRoomRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().message("gone").build());

        assertThatCode(() -> adapter("").deleteRoom(ROOM_ARN)).doesNotThrowAnyException();
    }
}
