package com.livecomerce.live.infrastructure.agora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import com.livecomerce.live.application.port.out.RoomEvent;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import com.livecomerce.live.infrastructure.RoomEventFailureReporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgoraRoomEventAdapterTest {

    @Mock AgoraRtmMessagePort      agoraRtmMessagePort;
    @Mock RoomEventFailureReporter failureReporter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID LIVE_PRODUCT_ID = UUID.randomUUID();

    private AgoraRoomEventAdapter adapter() {
        return new AgoraRoomEventAdapter(agoraRtmMessagePort, objectMapper, failureReporter);
    }

    private Live live() {
        return Live.create(UUID.randomUUID(), UUID.randomUUID(), LiveContext.STORE, "Live", null, null, 60);
    }

    /** Publishes the event and returns the payload sent to the live's channel. */
    private String publishAndCapture(Live live, RoomEvent event) {
        adapter().publish(live, event);
        var payload = ArgumentCaptor.forClass(String.class);
        verify(agoraRtmMessagePort).sendChannelMessage(eq("live-chat:" + live.getId()), payload.capture());
        return payload.getValue();
    }

    /** Asserts the payload is the exact JSON the pre-port call site produced (field order aside). */
    private void assertSameAsLegacy(String payload, Map<String, Object> legacy) throws Exception {
        assertThat(objectMapper.readTree(payload))
                .isEqualTo(objectMapper.readTree(objectMapper.writeValueAsString(legacy)));
    }

    @Test
    void publish_productPinned_sendsLegacyPayloadToLiveChatChannel() throws Exception {
        var live = live();

        var payload = publishAndCapture(live,
                new RoomEvent.ProductPinned(LIVE_PRODUCT_ID, "Shirt", new BigDecimal("99.50"), 60));

        assertThat(payload).isEqualTo("{\"type\":\"product-pinned\",\"liveProductId\":\"" + LIVE_PRODUCT_ID
                + "\",\"productName\":\"Shirt\",\"price\":99.50,\"displayDurationSeconds\":60}");
        assertSameAsLegacy(payload, Map.of(
                "type",                   "product-pinned",
                "liveProductId",          LIVE_PRODUCT_ID,
                "productName",            "Shirt",
                "price",                  new BigDecimal("99.50"),
                "displayDurationSeconds", 60));
    }

    @Test
    void publish_productExpired_sendsLegacyPayload() throws Exception {
        var live = live();

        var payload = publishAndCapture(live, new RoomEvent.ProductExpired(LIVE_PRODUCT_ID, "SOLD"));

        assertSameAsLegacy(payload, Map.of(
                "type",          "product-expired",
                "liveProductId", LIVE_PRODUCT_ID,
                "status",        "SOLD"));
    }

    @Test
    void publish_stockUpdate_sendsLegacyPayload() throws Exception {
        var live = live();

        var payload = publishAndCapture(live, new RoomEvent.StockUpdate(LIVE_PRODUCT_ID, 7));

        assertSameAsLegacy(payload, Map.of(
                "type",           "stock-update",
                "liveProductId",  LIVE_PRODUCT_ID,
                "stockRemaining", 7));
    }

    @Test
    void publish_viewerCount_sendsLegacyPayload() throws Exception {
        var live = live();

        var payload = publishAndCapture(live, new RoomEvent.ViewerCount(5L));

        assertThat(payload).isEqualTo("{\"type\":\"viewer-count\",\"count\":5}");
        assertSameAsLegacy(payload, Map.of("type", "viewer-count", "count", 5L));
    }

    @Test
    void publish_liveReconnecting_sendsLegacyPayload() throws Exception {
        var live = live();

        var payload = publishAndCapture(live, new RoomEvent.LiveReconnecting(live.getId(), "stream_disconnected"));

        assertSameAsLegacy(payload, Map.of(
                "type",   "live-reconnecting",
                "liveId", live.getId(),
                "reason", "stream_disconnected"));
    }

    @Test
    void publish_liveEnded_sendsLegacyPayload() throws Exception {
        var live = live();

        var payload = publishAndCapture(live, new RoomEvent.LiveEnded(live.getId()));

        assertSameAsLegacy(payload, Map.of("type", "live-ended", "liveId", live.getId()));
    }

    @Test
    void publish_rtmAccepted_returnsDelivered() {
        when(agoraRtmMessagePort.sendChannelMessage(any(), any())).thenReturn(true);

        assertThat(adapter().publish(live(), new RoomEvent.ViewerCount(1L))).isTrue();
        verifyNoInteractions(failureReporter);
    }

    @Test
    void publish_rtmRejected_returnsNotDelivered() {
        when(agoraRtmMessagePort.sendChannelMessage(any(), any())).thenReturn(false);

        assertThat(adapter().publish(live(), new RoomEvent.ViewerCount(1L))).isFalse();
    }

    @Test
    void publish_rtmThrows_returnsNotDelivered_andReportsWithoutThrowing() {
        var live    = live();
        var failure = new RuntimeException("RTM error");
        doThrow(failure).when(agoraRtmMessagePort).sendChannelMessage(any(), any());

        assertThat(adapter().publish(live, new RoomEvent.ViewerCount(1L))).isFalse();
        verify(failureReporter).report(live.getId(), "viewer-count", failure);
    }
}
