package com.livecomerce.live.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.application.port.out.ViewerCountPort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackViewerPresenceServiceTest {

    @Mock LoadLivePort        loadLivePort;
    @Mock SaveLivePort        saveLivePort;
    @Mock ViewerCountPort     viewerCountPort;
    @Mock AgoraRtmMessagePort agoraRtmMessagePort;

    TrackViewerPresenceService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sut = new TrackViewerPresenceService(loadLivePort, saveLivePort, viewerCountPort, agoraRtmMessagePort, new ObjectMapper());
    }

    private Live startedLive() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Test Live", null, null, 60);
        live.start();
        return live;
    }

    @Test
    void handleJoin_incrementsViewerCount_andBroadcastsViewerCount() throws Exception {
        var live = startedLive();
        String channelId = live.getAgoraChannelId();

        when(viewerCountPort.increment(live.getId())).thenReturn(5L);
        when(loadLivePort.loadByAgoraChannelId(channelId)).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenReturn(live);

        sut.handleJoin(channelId);

        verify(viewerCountPort).increment(live.getId());
        var captor = ArgumentCaptor.forClass(String.class);
        verify(agoraRtmMessagePort).sendChannelMessage(eq("live-chat:" + live.getId()), captor.capture());
        assertThat(captor.getValue()).contains("\"type\":\"viewer-count\"").contains("\"count\":5");
    }

    @Test
    void handleLeave_decrementsViewerCount_andBroadcastsViewerCount() throws Exception {
        var live = startedLive();
        String channelId = live.getAgoraChannelId();

        when(viewerCountPort.decrement(live.getId())).thenReturn(3L);
        when(loadLivePort.loadByAgoraChannelId(channelId)).thenReturn(Optional.of(live));

        sut.handleLeave(channelId);

        verify(viewerCountPort).decrement(live.getId());
        verify(agoraRtmMessagePort).sendChannelMessage(
                eq("live-chat:" + live.getId()),
                contains("\"count\":3"));
    }

    @Test
    void handleJoin_whenCountExceedsPeak_updatesPeakAndSaves() {
        var live = startedLive();
        String channelId = live.getAgoraChannelId();

        when(viewerCountPort.increment(live.getId())).thenReturn(10L);
        when(loadLivePort.loadByAgoraChannelId(channelId)).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenReturn(live);

        sut.handleJoin(channelId);

        verify(saveLivePort).save(live);
        assertThat(live.getPeakViewers()).isEqualTo(10);
    }

    @Test
    void handleJoin_unknownChannelId_logsWarnAndDoesNotThrow() {
        String unknownChannel = "unknown-channel-id";
        when(loadLivePort.loadByAgoraChannelId(unknownChannel)).thenReturn(Optional.empty());

        sut.handleJoin(unknownChannel);

        verifyNoInteractions(agoraRtmMessagePort);
        verifyNoInteractions(saveLivePort);
        verifyNoInteractions(viewerCountPort);
    }

    @Test
    void handleJoin_rtmExceptionIsSwallowed_joinStillSucceeds() throws Exception {
        var live = startedLive();
        String channelId = live.getAgoraChannelId();

        when(viewerCountPort.increment(live.getId())).thenReturn(1L);
        when(loadLivePort.loadByAgoraChannelId(channelId)).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenReturn(live);
        doThrow(new RuntimeException("RTM error")).when(agoraRtmMessagePort)
                .sendChannelMessage(any(), any());

        sut.handleJoin(channelId);
    }
}
