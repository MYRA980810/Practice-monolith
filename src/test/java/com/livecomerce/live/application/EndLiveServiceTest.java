package com.livecomerce.live.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.LiveEndedEvent;
import com.livecomerce.live.LiveReconnectingEvent;
import com.livecomerce.live.LiveRevivedEvent;
import com.livecomerce.live.application.port.in.EndLiveUseCase.EndLiveCommand;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
    @Mock LiveRoomCloser            liveRoomCloser;
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
        verify(liveRoomCloser).closeRoom(live);
        verifyNoInteractions(agoraRtmMessagePort);
    }

    @Test
    void endLive_closesRoomAfterSavingAndPublishingEndedEvent() {
        var live = ivsLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.endLive(new EndLiveCommand(live.getId(), SELLER_ID));

        InOrder inOrder = inOrder(saveLivePort, eventPublisher, liveRoomCloser);
        inOrder.verify(saveLivePort).save(live);
        inOrder.verify(eventPublisher).publishEvent(any(LiveEndedEvent.class));
        inOrder.verify(liveRoomCloser).closeRoom(live);
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
    void endStaleLive_transitionsToEnded_withoutSellerCheck() {
        var live = liveLive();
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.endStaleLive(live);

        assertThat(result.getStatus()).isEqualTo(LiveStatus.ENDED);
        verify(liveRoomCloser).closeRoom(live);
        var captor = ArgumentCaptor.forClass(LiveEndedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().liveId()).isEqualTo(live.getId());
        verifyNoInteractions(loadLivePort);
    }

    @Test
    void beginReconnecting_fromLiveStatus_transitionsToReconnecting() {
        var live = liveLive();
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.beginReconnecting(live);

        assertThat(result.getStatus()).isEqualTo(LiveStatus.RECONNECTING);
        assertThat(result.getStreamEndedAt()).isNotNull();
    }

    @Test
    void beginReconnecting_publishesLiveReconnectingEvent() {
        var live = liveLive();
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.beginReconnecting(live);

        var captor = ArgumentCaptor.forClass(LiveReconnectingEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.liveId()).isEqualTo(live.getId());
        assertThat(event.sellerId()).isEqualTo(SELLER_ID);
        assertThat(event.reason()).isEqualTo("stream_disconnected");
    }

    @Test
    void beginReconnecting_sendsAgoraRtmChannelMessage_withReasonField() {
        var live = liveLive();
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.beginReconnecting(live);

        verify(agoraRtmMessagePort).sendChannelMessage(
                eq("live-chat:" + live.getId()),
                contains("\"type\":\"live-reconnecting\""));
        verify(agoraRtmMessagePort).sendChannelMessage(
                eq("live-chat:" + live.getId()),
                contains("\"reason\":\"stream_disconnected\""));
    }

    @Test
    void beginReconnecting_rtmFailure_isSwallowed() {
        var live = liveLive();
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("RTM error")).when(agoraRtmMessagePort)
                .sendChannelMessage(any(), any());

        var result = sut.beginReconnecting(live);

        assertThat(result.getStatus()).isEqualTo(LiveStatus.RECONNECTING);
    }

    @Test
    void reviveLive_fromReconnectingStatus_transitionsToLiveAndSaves() {
        var live = liveLive();
        live.beginReconnecting();
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.reviveLive(live);

        assertThat(result.getStatus()).isEqualTo(LiveStatus.LIVE);
        assertThat(result.getStreamEndedAt()).isNull();
        verify(saveLivePort).save(live);
    }

    @Test
    void reviveLive_publishesLiveRevivedEvent() {
        var live = liveLive();
        live.beginReconnecting();
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.reviveLive(live);

        var captor = ArgumentCaptor.forClass(LiveRevivedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.liveId()).isEqualTo(live.getId());
        assertThat(event.sellerId()).isEqualTo(SELLER_ID);
        assertThat(event.storeId()).isEqualTo(STORE_ID);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void reviveLive_fromLiveStatus_throwsAndPublishesNothing() {
        var live = liveLive();

        assertThatThrownBy(() -> sut.reviveLive(live))
                .isInstanceOf(InvalidLiveStateException.class);
        verifyNoInteractions(saveLivePort, eventPublisher);
    }

    @Test
    void endStaleLive_fromReconnectingStatus_transitionsToEnded_withoutSellerCheck() {
        var live = liveLive();
        live.beginReconnecting();
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.endStaleLive(live);

        assertThat(result.getStatus()).isEqualTo(LiveStatus.ENDED);
        verifyNoInteractions(loadLivePort);
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
