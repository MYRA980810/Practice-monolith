package com.livecomerce.live.infrastructure.ivs;

import com.livecomerce.live.application.port.out.RoomEvent;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.infrastructure.RoomEventFailureReporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.services.ivschat.IvschatClient;
import software.amazon.awssdk.services.ivschat.model.SendEventRequest;
import software.amazon.awssdk.services.ivschat.model.SendEventResponse;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IvsRoomEventAdapterTest {

    @Mock IvschatClient            ivschatClient;
    @Mock RoomEventFailureReporter failureReporter;

    private static final String ROOM_ARN        = "arn:aws:ivschat:us-east-1:123:room/abc";
    private static final UUID   LIVE_PRODUCT_ID = UUID.randomUUID();

    private IvsRoomEventAdapter adapter() {
        return new IvsRoomEventAdapter(ivschatClient, failureReporter);
    }

    private Live liveWithRoom() {
        var live = Live.create(UUID.randomUUID(), "Live", null, null, 60);
        live.setChatRoom(ROOM_ARN);
        return live;
    }

    private SendEventRequest publishAndCapture(RoomEvent event) {
        adapter().publish(liveWithRoom(), event);
        var captor = ArgumentCaptor.forClass(SendEventRequest.class);
        verify(ivschatClient).sendEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    void publish_productPinned_sendsEventToLiveRoomWithStringAttributes() {
        var request = publishAndCapture(
                new RoomEvent.ProductPinned(LIVE_PRODUCT_ID, "Shirt", new BigDecimal("99.50"), 60));

        assertThat(request.roomIdentifier()).isEqualTo(ROOM_ARN);
        assertThat(request.eventName()).isEqualTo("product-pinned");
        assertThat(request.attributes()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "liveProductId",          LIVE_PRODUCT_ID.toString(),
                "productName",            "Shirt",
                "price",                  "99.50",
                "displayDurationSeconds", "60"));
    }

    @Test
    void publish_viewerCount_serializesNumberAsString() {
        var request = publishAndCapture(new RoomEvent.ViewerCount(5L));

        assertThat(request.eventName()).isEqualTo("viewer-count");
        assertThat(request.attributes()).containsExactlyEntriesOf(Map.of("count", "5"));
    }

    @Test
    void publish_liveEnded_sendsLiveIdAttribute() {
        var liveId  = UUID.randomUUID();
        var request = publishAndCapture(new RoomEvent.LiveEnded(liveId));

        assertThat(request.eventName()).isEqualTo("live-ended");
        assertThat(request.attributes()).containsExactlyEntriesOf(Map.of("liveId", liveId.toString()));
    }

    @Test
    void publish_chatUserUnbanned_sendsUserIdAttribute() {
        var userId  = UUID.randomUUID();
        var request = publishAndCapture(new RoomEvent.ChatUserUnbanned(userId));

        assertThat(request.eventName()).isEqualTo("chat-user-unbanned");
        assertThat(request.attributes()).containsExactlyEntriesOf(Map.of("userId", userId.toString()));
    }

    @Test
    void publish_hugeBigDecimal_usesPlainNotation() {
        var request = publishAndCapture(
                new RoomEvent.ProductPinned(LIVE_PRODUCT_ID, "Shirt", new BigDecimal("1E+3"), 60));

        assertThat(request.attributes()).containsEntry("price", "1000");
    }

    @Test
    void publish_success_returnsDelivered() {
        when(ivschatClient.sendEvent(any(SendEventRequest.class))).thenReturn(SendEventResponse.builder().build());

        assertThat(adapter().publish(liveWithRoom(), new RoomEvent.ViewerCount(1L))).isTrue();
        verifyNoInteractions(failureReporter);
    }

    @Test
    void publish_liveWithoutChatRoom_skipsWithoutCallingIvs_andCountsAsDelivered() {
        var live = Live.create(UUID.randomUUID(), "Live", null, null, 60);

        assertThat(adapter().publish(live, new RoomEvent.ViewerCount(1L))).isTrue();
        verifyNoInteractions(ivschatClient, failureReporter);
    }

    @Test
    void publish_sdkFailure_returnsNotDelivered_andReportsWithoutThrowing() {
        var live    = liveWithRoom();
        var failure = ApiCallTimeoutException.create(3000);
        when(ivschatClient.sendEvent(any(SendEventRequest.class))).thenThrow(failure);

        assertThat(adapter().publish(live, new RoomEvent.ViewerCount(1L))).isFalse();
        verify(failureReporter).report(live.getId(), "viewer-count", failure);
    }

    @Test
    void publish_unexpectedRuntimeFailure_returnsNotDelivered_andReportsWithoutThrowing() {
        var live    = liveWithRoom();
        var failure = new IllegalStateException("unexpected");
        when(ivschatClient.sendEvent(any(SendEventRequest.class))).thenThrow(failure);

        assertThat(adapter().publish(live, new RoomEvent.LiveEnded(live.getId()))).isFalse();
        verify(failureReporter).report(live.getId(), "live-ended", failure);
    }
}
