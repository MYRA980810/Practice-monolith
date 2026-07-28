package com.livecomerce.live.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.in.RecordViewerHeartbeatUseCase;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.application.port.out.ViewerCountPort;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotLiveException;
import com.livecomerce.live.domain.LiveStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class TrackViewerPresenceService implements RecordViewerHeartbeatUseCase {

    private static final Logger log = LoggerFactory.getLogger(TrackViewerPresenceService.class);

    private final LoadLivePort        loadLivePort;
    private final SaveLivePort        saveLivePort;
    private final ViewerCountPort     viewerCountPort;
    private final AgoraRtmMessagePort agoraRtmMessagePort;
    private final ObjectMapper        objectMapper;

    TrackViewerPresenceService(
            LoadLivePort loadLivePort,
            SaveLivePort saveLivePort,
            ViewerCountPort viewerCountPort,
            AgoraRtmMessagePort agoraRtmMessagePort,
            ObjectMapper objectMapper) {
        this.loadLivePort        = loadLivePort;
        this.saveLivePort        = saveLivePort;
        this.viewerCountPort     = viewerCountPort;
        this.agoraRtmMessagePort = agoraRtmMessagePort;
        this.objectMapper        = objectMapper;
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
        broadcastViewerCount(liveId, count);

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
        broadcastViewerCount(live.getId(), count);
    }

    @Override
    @Transactional
    public long recordHeartbeat(RecordHeartbeatCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        if (live.getStatus() != LiveStatus.LIVE) {
            throw new LiveNotLiveException(live.getId());
        }

        long count = viewerCountPort.heartbeat(command.liveId(), command.viewerId());
        broadcastViewerCount(command.liveId(), count);

        int previousPeak = live.getPeakViewers();
        live.updatePeakViewers((int) count);
        if (live.getPeakViewers() > previousPeak) {
            saveLivePort.save(live);
        }

        return count;
    }

    private void broadcastViewerCount(UUID liveId, long count) {
        try {
            String payload = objectMapper.writeValueAsString(
                    Map.of("type", "viewer-count", "count", count));
            agoraRtmMessagePort.sendChannelMessage("live-chat:" + liveId, payload);
        } catch (Exception e) {
            log.warn("Agora RTM viewer-count broadcast failed: {}", e.getMessage());
        }
    }
}
