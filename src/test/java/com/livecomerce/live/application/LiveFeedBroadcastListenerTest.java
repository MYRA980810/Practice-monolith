package com.livecomerce.live.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.LiveEndedEvent;
import com.livecomerce.live.LiveStartedEvent;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LiveFeedBroadcastListenerTest {

    @Mock AgoraRtmMessagePort agoraRtmMessagePort;
    @Spy  ObjectMapper        objectMapper = new ObjectMapper();
    @InjectMocks LiveFeedBroadcastListener sut;

    private static final UUID LIVE_ID  = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();

    @Test
    void onLiveStarted_sendsAddSignalToFeedChannel() {
        var event = new LiveStartedEvent(LIVE_ID, STORE_ID, "Flash Sale", List.of());

        sut.on(event);

        var captor = ArgumentCaptor.forClass(String.class);
        verify(agoraRtmMessagePort).sendChannelMessage(eq("lives-feed"), captor.capture());
        assertThat(captor.getValue())
                .contains("\"type\":\"live-started\"")
                .contains("\"liveId\":\"" + LIVE_ID + "\"");
    }

    @Test
    void onLiveEnded_sendsRemoveSignalToFeedChannel() {
        var event = new LiveEndedEvent(LIVE_ID, SELLER_ID);

        sut.on(event);

        var captor = ArgumentCaptor.forClass(String.class);
        verify(agoraRtmMessagePort).sendChannelMessage(eq("lives-feed"), captor.capture());
        assertThat(captor.getValue())
                .contains("\"type\":\"live-ended\"")
                .contains("\"liveId\":\"" + LIVE_ID + "\"");
    }

    @Test
    void onLiveStarted_rtmExceptionIsSwallowed() {
        var event = new LiveStartedEvent(LIVE_ID, STORE_ID, "Flash Sale", List.of());
        doThrow(new RuntimeException("RTM error")).when(agoraRtmMessagePort)
                .sendChannelMessage(any(), any());

        sut.on(event);
    }
}
