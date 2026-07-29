package com.livecomerce.live.application;

import com.livecomerce.live.LiveStartedEvent;
import com.livecomerce.live.ProfileCompletionPort;
import com.livecomerce.live.application.port.in.StartLiveUseCase.StartLiveCommand;
import com.livecomerce.live.application.port.out.AgoraTokenPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveSubscriptionPort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.application.port.out.SaveLiveSubscriptionPort;
import com.livecomerce.live.application.port.out.SellerIvsChannelPort;
import com.livecomerce.live.application.port.out.VideoBroadcastPort;
import com.livecomerce.live.application.port.out.VideoBroadcastPort.ChannelHandle;
import com.livecomerce.live.domain.*;
import org.junit.jupiter.api.BeforeEach;
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
    @Mock ProfileCompletionPort       profileCompletionPort;
    @Mock SellerIvsChannelPort        sellerIvsChannelPort;
    @InjectMocks StartLiveService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(profileCompletionPort.isProfileComplete(any())).thenReturn(true);
    }

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
        when(sellerIvsChannelPort.loadBySellerId(SELLER_ID)).thenReturn(Optional.empty());
        when(videoBroadcastPort.createChannel("seller-" + SELLER_ID)).thenReturn(
                new ChannelHandle("arn:ivs:channel", "rtmps://ingest", "arn:ivs:key",
                        "sk_stream_key", "https://playback.url"));
        when(sellerIvsChannelPort.trySave(any())).thenReturn(true);
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
        verify(videoBroadcastPort).createChannel("seller-" + SELLER_ID);
        verify(sellerIvsChannelPort).trySave(any());
        verifyNoInteractions(agoraTokenPort);
    }

    @Test
    void startLive_withIvsProvider_whenChannelAlreadyExistsForSeller_reusesItAndNeverCallsCreateChannel() {
        ReflectionTestUtils.setField(sut, "videoProvider", "ivs");

        var live = scheduledLive();
        var existingHandle = new ChannelHandle("arn:ivs:existing", "rtmps://existing", "arn:ivs:existing-key",
                "sk_existing_key", "https://existing.playback.url");
        var existingChannel = SellerIvsChannel.create(SELLER_ID, existingHandle);
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(sellerIvsChannelPort.loadBySellerId(SELLER_ID)).thenReturn(Optional.of(existingChannel));
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd    = new StartLiveCommand(live.getId(), SELLER_ID, "uid-42");
        var result = sut.startLive(cmd);

        assertThat(result.getIvsChannelArn()).isEqualTo("arn:ivs:existing");
        verifyNoInteractions(videoBroadcastPort);
        verify(sellerIvsChannelPort, never()).trySave(any());
    }

    @Test
    void startLive_withIvsProvider_whenTrySaveLosesRace_reloadsWinningChannel() {
        ReflectionTestUtils.setField(sut, "videoProvider", "ivs");

        var live = scheduledLive();
        var createdHandle = new ChannelHandle("arn:ivs:created", "rtmps://created", "arn:ivs:created-key",
                "sk_created_key", "https://created.playback.url");
        var winningHandle = new ChannelHandle("arn:ivs:winner", "rtmps://winner", "arn:ivs:winner-key",
                "sk_winner_key", "https://winner.playback.url");
        var winningChannel = SellerIvsChannel.create(SELLER_ID, winningHandle);
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(sellerIvsChannelPort.loadBySellerId(SELLER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winningChannel));
        when(videoBroadcastPort.createChannel("seller-" + SELLER_ID)).thenReturn(createdHandle);
        when(sellerIvsChannelPort.trySave(any())).thenReturn(false);
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd    = new StartLiveCommand(live.getId(), SELLER_ID, "uid-42");
        var result = sut.startLive(cmd);

        assertThat(result.getIvsChannelArn()).isEqualTo("arn:ivs:winner");
        assertThat(result.getIvsIngestEndpoint()).isEqualTo("rtmps://winner");
        assertThat(result.getIvsStreamKeyArn()).isEqualTo("arn:ivs:winner-key");
        assertThat(result.getIvsStreamKeyValue()).isEqualTo("sk_winner_key");
        assertThat(result.getIvsPlaybackUrl()).isEqualTo("https://winner.playback.url");
        verify(sellerIvsChannelPort, times(2)).loadBySellerId(SELLER_ID);
    }

    @Test
    void startLive_withIvsProvider_whenCreateChannelFails_throwsVideoProviderUnavailableAndDoesNotSave() {
        ReflectionTestUtils.setField(sut, "videoProvider", "ivs");

        var live = scheduledLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(sellerIvsChannelPort.loadBySellerId(SELLER_ID)).thenReturn(Optional.empty());
        when(videoBroadcastPort.createChannel("seller-" + SELLER_ID))
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

    @Test
    void startLive_withIncompleteProfile_throwsProfileIncompleteException() {
        var live = scheduledLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(profileCompletionPort.isProfileComplete(SELLER_ID)).thenReturn(false);

        var cmd = new StartLiveCommand(live.getId(), SELLER_ID, "uid-42");

        assertThatThrownBy(() -> sut.startLive(cmd))
                .isInstanceOf(ProfileIncompleteException.class);
        verify(saveLivePort, never()).save(any());
    }
}
