package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.RecordViewerHeartbeatUseCase;
import com.livecomerce.live.application.port.out.LiveRoomEventPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.RoomEvent;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.application.port.out.ViewerCountPort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotLiveException;
import com.livecomerce.live.domain.LiveStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TrackViewerPresenceService implements RecordViewerHeartbeatUseCase {

    private static final Logger log = LoggerFactory.getLogger(TrackViewerPresenceService.class);

    private final LoadLivePort        loadLivePort;
    private final SaveLivePort        saveLivePort;
    private final ViewerCountPort     viewerCountPort;
    private final LiveRoomEventPort   liveRoomEventPort;

    TrackViewerPresenceService(
            LoadLivePort loadLivePort,
            SaveLivePort saveLivePort,
            ViewerCountPort viewerCountPort,
            LiveRoomEventPort liveRoomEventPort) {
        this.loadLivePort        = loadLivePort;
        this.saveLivePort        = saveLivePort;
        this.viewerCountPort     = viewerCountPort;
        this.liveRoomEventPort   = liveRoomEventPort;
    }

    @Transactional
    public void handleJoin(String agoraChannelId) {
        var liveOpt = loadLivePort.loadByAgoraChannelId(agoraChannelId);
        if (liveOpt.isEmpty()) {
            log.warn("TrackViewerPresence: no live found for agoraChannelId={}", agoraChannelId);
            return;
        }

        var live  = liveOpt.get();
        UUID liveId = live.getId();
        long count = viewerCountPort.increment(liveId);
        broadcastViewerCount(live, count);

        live.updatePeakViewers((int) count);
        saveLivePort.save(live);
    }

    @Transactional
    public void handleLeave(String agoraChannelId) {
        var liveOpt = loadLivePort.loadByAgoraChannelId(agoraChannelId);
        if (liveOpt.isEmpty()) {
            log.warn("TrackViewerPresence: no live found for agoraChannelId={}", agoraChannelId);
            return;
        }

        var live  = liveOpt.get();
        long count = viewerCountPort.decrement(live.getId());
        broadcastViewerCount(live, count);
    }

    @Override
    @Transactional
    public long recordHeartbeat(RecordHeartbeatCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        if (live.getStatus() != LiveStatus.LIVE && live.getStatus() != LiveStatus.RECONNECTING) {
            throw new LiveNotLiveException(live.getId());
        }

        long count = viewerCountPort.heartbeat(command.liveId(), command.viewerId());
        broadcastViewerCount(live, count);

        int previousPeak = live.getPeakViewers();
        live.updatePeakViewers((int) count);
        if (live.getPeakViewers() > previousPeak) {
            saveLivePort.save(live);
        }

        return count;
    }

    private void broadcastViewerCount(Live live, long count) {
        if (!viewerCountPort.shouldBroadcast(live.getId(), count)) {
            return;
        }
        if (!liveRoomEventPort.publish(live, new RoomEvent.ViewerCount(count))) {
            // shouldBroadcast already recorded this count; undo it so the next heartbeat retries.
            viewerCountPort.resetLastBroadcast(live.getId(), count);
        }
    }
}
