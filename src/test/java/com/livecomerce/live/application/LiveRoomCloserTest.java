package com.livecomerce.live.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import com.livecomerce.live.application.port.out.VideoBroadcastPort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LiveRoomCloserTest {

    @Mock AgoraRtmMessagePort agoraRtmMessagePort;
    @Mock VideoBroadcastPort  videoBroadcastPort;
    @Spy  ObjectMapper        objectMapper = new ObjectMapper();
    @InjectMocks LiveRoomCloser sut;

    private static final String CHANNEL_ARN = "arn:ivs:channel";

    private Live live() {
        var live = Live.create(UUID.randomUUID(), UUID.randomUUID(), LiveContext.STORE, "My Live", null, null, 60);
        live.start();
        return live;
    }

    private Live ivsLive() {
        var live = live();
        live.setIvsChannel(CHANNEL_ARN, "rtmps://ingest", "arn:ivs:key", "sk_stream_key", "https://playback.url");
        return live;
    }

    @Test
    @SuppressWarnings("unchecked")
    void closeRoom_sendsLiveEndedMessageToLiveChatChannel() throws Exception {
        var live = live();

        sut.closeRoom(live);

        var payload = ArgumentCaptor.forClass(String.class);
        verify(agoraRtmMessagePort).sendChannelMessage(eq("live-chat:" + live.getId()), payload.capture());
        Map<String, Object> body = objectMapper.readValue(payload.getValue(), Map.class);
        assertThat(body).containsOnly(
                Map.entry("type", "live-ended"),
                Map.entry("liveId", live.getId().toString()));
    }

    @Test
    void closeRoom_withIvsChannel_stopsIvsStream() {
        var live = ivsLive();

        sut.closeRoom(live);

        verify(videoBroadcastPort).stopStream(CHANNEL_ARN);
    }

    @Test
    void closeRoom_withoutIvsChannel_skipsStopStream() {
        sut.closeRoom(live());

        verifyNoInteractions(videoBroadcastPort);
    }

    @Test
    void closeRoom_stopStreamFailure_isSwallowed() {
        var live = ivsLive();
        doThrow(new RuntimeException("IVS error")).when(videoBroadcastPort).stopStream(CHANNEL_ARN);

        assertThatCode(() -> sut.closeRoom(live)).doesNotThrowAnyException();
    }

    @Test
    void closeRoom_rtmFailure_isSwallowed_andStillStopsIvsStream() {
        var live = ivsLive();
        doThrow(new RuntimeException("RTM error")).when(agoraRtmMessagePort).sendChannelMessage(any(), any());

        assertThatCode(() -> sut.closeRoom(live)).doesNotThrowAnyException();
        verify(videoBroadcastPort).stopStream(CHANNEL_ARN);
    }
}
