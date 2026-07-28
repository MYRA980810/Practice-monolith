package com.livecomerce.live.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.LiveEndedEvent;
import com.livecomerce.live.application.port.in.EndLiveUseCase.EndLiveCommand;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.application.port.out.VideoBroadcastPort;
import com.livecomerce.live.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndLiveServiceTest {

    @Mock LoadLivePort              loadLivePort;
    @Mock SaveLivePort              saveLivePort;
    @Mock AgoraRtmMessagePort       agoraRtmMessagePort;
    @Mock VideoBroadcastPort        videoBroadcastPort;
    @Mock ApplicationEventPublisher eventPublisher;
    @Spy  ObjectMapper              objectMapper = new ObjectMapper();
    @InjectMocks EndLiveService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();

    private Live liveLive() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        live.start();
        return live;
    }

    private Live ivsLive() {
        var live = liveLive();
        live.setIvsChannel("arn:ivs:channel", "rtmps://ingest", "arn:ivs:key", "sk_stream_key", "https://playback.url");
        return live;
    }

    @Test
    void endLive_fromLiveStatus_transitionsToEnded() {
        var live = liveLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.endLive(new EndLiveCommand(live.getId(), SELLER_ID));

        assertThat(result.getStatus()).isEqualTo(LiveStatus.ENDED);
        assertThat(result.getEndedAt()).isNotNull();
        verify(agoraRtmMessagePort).sendChannelMessage(
                eq("live-chat:" + live.getId()),
                contains("\"type\":\"live-ended\""));
        verifyNoInteractions(videoBroadcastPort);
    }

    @Test
    void endLive_withIvsChannel_stopsIvsStream() {
        var live = ivsLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.endLive(new EndLiveCommand(live.getId(), SELLER_ID));

        verify(videoBroadcastPort).stopStream("arn:ivs:channel");
    }

    @Test
    void endLive_withIvsChannel_stopStreamFailure_isSwallowed() {
        var live = ivsLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("IVS error")).when(videoBroadcastPort).stopStream("arn:ivs:channel");

        var result = sut.endLive(new EndLiveCommand(live.getId(), SELLER_ID));

        assertThat(result.getStatus()).isEqualTo(LiveStatus.ENDED);
    }

    @Test
    void endLive_fromLiveStatus_publishesLiveEndedEvent() {
        var live = liveLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.endLive(new EndLiveCommand(live.getId(), SELLER_ID));

        var captor = ArgumentCaptor.forClass(LiveEndedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.liveId()).isEqualTo(live.getId());
        assertThat(event.sellerId()).isEqualTo(SELLER_ID);
    }

    @Test
    void endLive_wrongSeller_throwsLiveNotOwned() {
        var live        = liveLive();
        var wrongSeller = UUID.randomUUID();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        assertThatThrownBy(() -> sut.endLive(new EndLiveCommand(live.getId(), wrongSeller)))
                .isInstanceOf(LiveNotOwnedBySellerException.class);
    }
}
