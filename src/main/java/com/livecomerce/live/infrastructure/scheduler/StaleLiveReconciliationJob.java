package com.livecomerce.live.infrastructure.scheduler;

import com.livecomerce.live.application.EndLiveService;
import com.livecomerce.live.application.port.out.LoadLivePort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Reconciles lives whose IVS stream reported a disconnect ({@link
 * com.livecomerce.live.infrastructure.ivs.IvsStreamEventListener}) — the
 * seller's client crashed, lost network, or was closed without calling the
 * explicit end endpoint. A live past the first grace period moves to
 * RECONNECTING (a second, longer window to come back without losing its
 * {@code liveId}); one past the reconnect timeout is closed for good,
 * otherwise it would stay LIVE/RECONNECTING (and visible on the buyer feed)
 * forever.
 */
@Component
@RequiredArgsConstructor
class StaleLiveReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(StaleLiveReconciliationJob.class);

    private final LoadLivePort  loadLivePort;
    private final EndLiveService endLiveService;

    @Value("${live.stream-ended-grace-period-seconds:180}")
    private int gracePeriodSeconds;

    @Value("${live.reconnect-timeout-seconds:600}")
    private int reconnectTimeoutSeconds;

    /**
     * Not itself {@code @Transactional} — each {@code endLiveService} call already
     * carries its own transaction (class-level on {@link EndLiveService}), and each
     * item must commit and broadcast independently: this method fires an
     * irreversible RTM message per live, so one live failing to transition must
     * not roll back or block the ones already processed in the same tick.
     */
    @Scheduled(fixedDelayString = "${live.stale-check-ms:60000}")
    void closeStaleLives() {
        var cutoff = Instant.now().minusSeconds(gracePeriodSeconds);
        var stale  = loadLivePort.loadStaleLive(cutoff);

        if (stale.isEmpty()) {
            return;
        }

        log.info("Stale live reconciliation: {} live(s) past the {}s grace period, marking as reconnecting",
                stale.size(), gracePeriodSeconds);

        for (var live : stale) {
            try {
                endLiveService.beginReconnecting(live);
                log.info("Live {} moved to RECONNECTING (streamEndedAt={})", live.getId(), live.getStreamEndedAt());
            } catch (Exception e) {
                log.warn("Failed to move live {} to RECONNECTING: {}", live.getId(), e.getMessage());
            }
        }
    }

    /** See {@link #closeStaleLives()} for why this isn't {@code @Transactional} either. */
    @Scheduled(fixedDelayString = "${live.stale-check-ms:60000}")
    void closeStaleReconnectingLives() {
        var cutoff = Instant.now().minusSeconds(reconnectTimeoutSeconds);
        var stale  = loadLivePort.loadStaleReconnecting(cutoff);

        if (stale.isEmpty()) {
            return;
        }

        log.info("Stale live reconciliation: {} live(s) past the {}s reconnect timeout, auto-ending",
                stale.size(), reconnectTimeoutSeconds);

        for (var live : stale) {
            try {
                endLiveService.endStaleLive(live);
                log.info("Auto-ended stale reconnecting live {} (streamEndedAt={})", live.getId(), live.getStreamEndedAt());
            } catch (Exception e) {
                log.warn("Failed to end stale reconnecting live {}: {}", live.getId(), e.getMessage());
            }
        }
    }
}
