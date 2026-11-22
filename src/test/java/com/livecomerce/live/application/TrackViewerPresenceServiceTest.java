package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.RecordViewerHeartbeatUseCase;
import com.livecomerce.live.application.port.out.LiveRoomEventPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.RoomEvent;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.application.port.out.ViewerCountPort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotLiveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackViewerPresenceServiceTest {

    @Mock LoadLivePort        loadLivePort;
    @Mock SaveLivePort        saveLivePort;
    @Mock ViewerCountPort     viewerCountPort;
    @Mock LiveRoomEventPort   liveRoomEventPort;

    TrackViewerPresenceService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sut = new TrackViewerPresenceService(loadLivePort, saveLivePort, viewerCountPort, liveRoomEventPort);
    }

    private Live startedLive() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Test Live", null, null, 60);
        live.start();
        return live;
    }

    private Live scheduledLive() {
        return Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Test Live", null, null, 60);
    }

    @Test
    void handleJoin_incrementsViewerCount_andBroadcastsViewerCount() throws Exception {
        var live = startedLive();
        String channelId = live.getAgoraChannelId();

        when(viewerCountPort.increment(live.getId())).thenReturn(5L);
        when(viewerCountPort.shouldBroadcast(live.getId(), 5L)).thenReturn(true);
        when(loadLivePort.loadByAgoraChannelId(channelId)).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenReturn(live);

        sut.handleJoin(channelId);

        verify(viewerCountPort).increment(live.getId());
        verify(liveRoomEventPort).publish(live, new RoomEvent.ViewerCount(5L));
    }

    @Test
    void handleLeave_decrementsViewerCount_andBroadcastsViewerCount() throws Exception {
        var live = startedLive();
        String channelId = live.getAgoraChannelId();

        when(viewerCountPort.decrement(live.getId())).thenReturn(3L);
        when(viewerCountPort.shouldBroadcast(live.getId(), 3L)).thenReturn(true);
        when(loadLivePort.loadByAgoraChannelId(channelId)).thenReturn(Optional.of(live));

        sut.handleLeave(channelId);

        verify(viewerCountPort).decrement(live.getId());
        verify(liveRoomEventPort).publish(live, new RoomEvent.ViewerCount(3L));
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

        verifyNoInteractions(liveRoomEventPort);
        verifyNoInteractions(saveLivePort);
        verifyNoInteractions(viewerCountPort);
    }

    @Test
    void handleJoin_roomEventNotDelivered_resetsLastBroadcastSoNextHeartbeatRetries() {
        var live = startedLive();
        String channelId = live.getAgoraChannelId();

        when(viewerCountPort.increment(live.getId())).thenReturn(1L);
        when(viewerCountPort.shouldBroadcast(live.getId(), 1L)).thenReturn(true);
        when(loadLivePort.loadByAgoraChannelId(channelId)).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenReturn(live);
        when(liveRoomEventPort.publish(any(), any())).thenReturn(false);

        sut.handleJoin(channelId);

        verify(viewerCountPort).resetLastBroadcast(live.getId(), 1L);
    }

    @Test
    void recordHeartbeat_roomEventDelivered_keepsLastBroadcast() {
        var live = startedLive();
        String viewerId = "viewer-abc";

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(viewerCountPort.heartbeat(live.getId(), viewerId)).thenReturn(4L);
        when(viewerCountPort.shouldBroadcast(live.getId(), 4L)).thenReturn(true);
        when(liveRoomEventPort.publish(live, new RoomEvent.ViewerCount(4L))).thenReturn(true);
        when(saveLivePort.save(any())).thenReturn(live);

        sut.recordHeartbeat(new RecordViewerHeartbeatUseCase.RecordHeartbeatCommand(live.getId(), viewerId));

        verify(viewerCountPort, never()).resetLastBroadcast(any(), anyLong());
    }

    // --- recordHeartbeat ---

    @Test
    void recordHeartbeat_registersViewerAndBroadcastsCount() {
        var live = startedLive();
        String viewerId = "viewer-abc";

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(viewerCountPort.heartbeat(live.getId(), viewerId)).thenReturn(4L);
        when(viewerCountPort.shouldBroadcast(live.getId(), 4L)).thenReturn(true);
        when(saveLivePort.save(any())).thenReturn(live);

        long count = sut.recordHeartbeat(new RecordViewerHeartbeatUseCase.RecordHeartbeatCommand(live.getId(), viewerId));

        assertThat(count).isEqualTo(4L);
        verify(viewerCountPort).heartbeat(live.getId(), viewerId);
        verify(liveRoomEventPort).publish(live, new RoomEvent.ViewerCount(4L));
    }

    @Test
    void recordHeartbeat_whenCountUnchanged_skipsBroadcast() {
        var live = startedLive();
        String viewerId = "viewer-abc";

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(viewerCountPort.heartbeat(live.getId(), viewerId)).thenReturn(4L);
        when(viewerCountPort.shouldBroadcast(live.getId(), 4L)).thenReturn(false);

        long count = sut.recordHeartbeat(new RecordViewerHeartbeatUseCase.RecordHeartbeatCommand(live.getId(), viewerId));

        assertThat(count).isEqualTo(4L);
        verify(liveRoomEventPort, never()).publish(any(), any());
    }

    @Test
    void recordHeartbeat_whenCountExceedsPeak_updatesPeakAndSaves() {
        var live = startedLive();
        String viewerId = "viewer-abc";

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(viewerCountPort.heartbeat(live.getId(), viewerId)).thenReturn(12L);
        when(saveLivePort.save(any())).thenReturn(live);

        sut.recordHeartbeat(new RecordViewerHeartbeatUseCase.RecordHeartbeatCommand(live.getId(), viewerId));

        verify(saveLivePort).save(live);
        assertThat(live.getPeakViewers()).isEqualTo(12);
    }

    @Test
    void recordHeartbeat_liveReconnecting_succeeds() {
        var live = startedLive();
        live.beginReconnecting();
        String viewerId = "viewer-abc";

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(viewerCountPort.heartbeat(live.getId(), viewerId)).thenReturn(4L);
        when(viewerCountPort.shouldBroadcast(live.getId(), 4L)).thenReturn(true);
        when(saveLivePort.save(any())).thenReturn(live);

        long count = sut.recordHeartbeat(new RecordViewerHeartbeatUseCase.RecordHeartbeatCommand(live.getId(), viewerId));

        assertThat(count).isEqualTo(4L);
    }

    @Test
    void recordHeartbeat_liveNotFound_throwsLiveNotFound() {
        var liveId = UUID.randomUUID();
        when(loadLivePort.loadById(liveId)).thenReturn(Optional.empty());

        var cmd = new RecordViewerHeartbeatUseCase.RecordHeartbeatCommand(liveId, "viewer-abc");

        assertThatThrownBy(() -> sut.recordHeartbeat(cmd))
                .isInstanceOf(LiveNotFoundException.class);
        verifyNoInteractions(viewerCountPort, saveLivePort, liveRoomEventPort);
    }

    @Test
    void recordHeartbeat_liveNotLive_throwsLiveNotLiveException() {
        var live = scheduledLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        var cmd = new RecordViewerHeartbeatUseCase.RecordHeartbeatCommand(live.getId(), "viewer-abc");

        assertThatThrownBy(() -> sut.recordHeartbeat(cmd))
                .isInstanceOf(LiveNotLiveException.class);
        verifyNoInteractions(viewerCountPort, saveLivePort);
    }

    @Test
    void recordHeartbeat_whenCountDoesNotExceedPeak_doesNotSave() {
        var live = startedLive();
        live.updatePeakViewers(20);
        String viewerId = "viewer-abc";

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(viewerCountPort.heartbeat(live.getId(), viewerId)).thenReturn(5L);

        long count = sut.recordHeartbeat(new RecordViewerHeartbeatUseCase.RecordHeartbeatCommand(live.getId(), viewerId));

        assertThat(count).isEqualTo(5L);
        assertThat(live.getPeakViewers()).isEqualTo(20);
        verify(saveLivePort, never()).save(any());
    }
}
