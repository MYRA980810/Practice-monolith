package com.livecomerce.live.application;

import com.livecomerce.live.LiveStartedEvent;
import com.livecomerce.live.application.port.in.StartLiveUseCase.StartLiveCommand;
import com.livecomerce.live.application.port.out.AgoraTokenPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveSubscriptionPort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.application.port.out.SaveLiveSubscriptionPort;
import com.livecomerce.live.application.port.out.VideoBroadcastPort;
import com.livecomerce.live.application.port.out.VideoBroadcastPort.ChannelHandle;
import com.livecomerce.live.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartLiveServiceTest {

    @Mock LoadLivePort                loadLivePort;
    @Mock SaveLivePort                saveLivePort;
    @Mock AgoraTokenPort              agoraTokenPort;
    @Mock VideoBroadcastPort          videoBroadcastPort;
    @Mock LoadLiveSubscriptionPort    loadLiveSubscriptionPort;
    @Mock SaveLiveSubscriptionPort    saveLiveSubscriptionPort;
    @Mock ApplicationEventPublisher   eventPublisher;
    @InjectMocks StartLiveService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();

    private Live scheduledLive() {
        return Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
    }

    @Test
    void startLive_generatesTokenAndSetsStreamToken() {
        var live = scheduledLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(agoraTokenPort.generateRtcToken(anyString(), anyString(), anyInt())).thenReturn("agora-token-abc");
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd    = new StartLiveCommand(live.getId(), SELLER_ID, "uid-42");
        var result = sut.startLive(cmd);

        assertThat(result.getStatus()).isEqualTo(LiveStatus.LIVE);
        assertThat(result.getStreamToken()).isEqualTo("agora-token-abc");
        verify(agoraTokenPort).generateRtcToken(live.getAgoraChannelId(), "uid-42", 3600);
        verifyNoInteractions(videoBroadcastPort);
    }

    @Test
    void startLive_withIvsProvider_createsIvsChannelAndSkipsAgora() {
        ReflectionTestUtils.setField(sut, "videoProvider", "ivs");

        var live = scheduledLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(videoBroadcastPort.createChannel("live-" + live.getId())).thenReturn(
                new ChannelHandle("arn:ivs:channel", "rtmps://ingest", "arn:ivs:key",
                        "sk_stream_key", "https://playback.url"));
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd    = new StartLiveCommand(live.getId(), SELLER_ID, "uid-42");
        var result = sut.startLive(cmd);

        assertThat(result.getStatus()).isEqualTo(LiveStatus.LIVE);
        assertThat(result.getIvsChannelArn()).isEqualTo("arn:ivs:channel");
        assertThat(result.getIvsIngestEndpoint()).isEqualTo("rtmps://ingest");
        assertThat(result.getIvsStreamKeyArn()).isEqualTo("arn:ivs:key");
        assertThat(result.getIvsStreamKeyValue()).isEqualTo("sk_stream_key");
        assertThat(result.getIvsPlaybackUrl()).isEqualTo("https://playback.url");
        assertThat(result.getStreamToken()).isNull();
        verify(videoBroadcastPort).createChannel("live-" + live.getId());
        verifyNoInteractions(agoraTokenPort);
    }

    @Test
    void startLive_withIvsProvider_whenCreateChannelFails_throwsVideoProviderUnavailableAndDoesNotSave() {
        ReflectionTestUtils.setField(sut, "videoProvider", "ivs");

        var live = scheduledLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(videoBroadcastPort.createChannel("live-" + live.getId()))
                .thenThrow(new RuntimeException("AWS IVS throttled"));

        var cmd = new StartLiveCommand(live.getId(), SELLER_ID, "uid-42");

        assertThatThrownBy(() -> sut.startLive(cmd))
                .isInstanceOf(VideoProviderUnavailableException.class);
        verify(saveLivePort, never()).save(any());
    }

    @Test
    void startLive_wrongSeller_throwsLiveNotOwned() {
        var live      = scheduledLive();
        var wrongSeller = UUID.randomUUID();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        var cmd = new StartLiveCommand(live.getId(), wrongSeller, "uid-42");

        assertThatThrownBy(() -> sut.startLive(cmd))
                .isInstanceOf(LiveNotOwnedBySellerException.class);
    }

    @Test
    void startLive_liveNotFound_throwsLiveNotFound() {
        var liveId = UUID.randomUUID();
        when(loadLivePort.loadById(liveId)).thenReturn(Optional.empty());

        var cmd = new StartLiveCommand(liveId, SELLER_ID, "uid-42");

        assertThatThrownBy(() -> sut.startLive(cmd))
                .isInstanceOf(LiveNotFoundException.class);
    }

    @Test
    void startLive_notScheduledStatus_throwsInvalidLiveState() {
        var live = scheduledLive();
        live.start(); // already started — token is already set
        live.setStreamToken("prev-token");
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        // NOTE: agoraTokenPort is NOT stubbed — live.start() throws before token generation

        var cmd = new StartLiveCommand(live.getId(), SELLER_ID, "uid-42");

        assertThatThrownBy(() -> sut.startLive(cmd))
                .isInstanceOf(InvalidLiveStateException.class);
    }

    @Test
    @SuppressWarnings("null")
    void startLive_withSubscribers_publishesEventAndDeletesSubscriptions() {
        var live = scheduledLive();
        var subscriber1 = UUID.randomUUID();
        var subscriber2 = UUID.randomUUID();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(agoraTokenPort.generateRtcToken(anyString(), anyString(), anyInt())).thenReturn("token");
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loadLiveSubscriptionPort.loadSubscriberIdsByLiveId(live.getId()))
                .thenReturn(List.of(subscriber1, subscriber2));

        sut.startLive(new StartLiveCommand(live.getId(), SELLER_ID, "uid-1"));

        var captor = ArgumentCaptor.forClass(LiveStartedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.liveId()).isEqualTo(live.getId());
        assertThat(event.subscriberIds()).containsExactlyInAnyOrder(subscriber1, subscriber2);
        verify(saveLiveSubscriptionPort).deleteAllByLiveId(live.getId());
    }

    @Test
    void startLive_withNoSubscribers_publishesEventWithEmptyList() {
        var live = scheduledLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(agoraTokenPort.generateRtcToken(anyString(), anyString(), anyInt())).thenReturn("token");
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loadLiveSubscriptionPort.loadSubscriberIdsByLiveId(live.getId())).thenReturn(List.of());

        sut.startLive(new StartLiveCommand(live.getId(), SELLER_ID, "uid-1"));

        var captor = ArgumentCaptor.forClass(LiveStartedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().subscriberIds()).isEmpty();
        verify(saveLiveSubscriptionPort).deleteAllByLiveId(live.getId());
    }
}
