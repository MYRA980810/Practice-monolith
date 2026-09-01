package com.livecomerce.live.infrastructure.scheduler;

import com.livecomerce.live.application.EndLiveService;
import com.livecomerce.live.application.port.out.LoadLivePort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Closes lives whose IVS stream reported a disconnect ({@link
 * com.livecomerce.live.infrastructure.ivs.IvsStreamEventListener}) and never
 * reconnected within the grace period — the seller's client crashed, lost
 * network, or was closed without calling the explicit end endpoint, so the
 * live would otherwise stay LIVE (and visible on the buyer feed) forever.
 */
@Component
@RequiredArgsConstructor
class StaleLiveReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(StaleLiveReconciliationJob.class);

    private final LoadLivePort  loadLivePort;
    private final EndLiveService endLiveService;

    @Value("${live.stream-ended-grace-period-seconds:180}")
    private int gracePeriodSeconds;

    @Scheduled(fixedDelayString = "${live.stale-check-ms:60000}")
    @Transactional
    void closeStaleLives() {
        var cutoff = Instant.now().minusSeconds(gracePeriodSeconds);
        var stale  = loadLivePort.loadStaleLive(cutoff);

        if (stale.isEmpty()) {
            return;
        }

        log.info("Stale live reconciliation: {} live(s) past the {}s grace period, auto-ending",
                stale.size(), gracePeriodSeconds);

        for (var live : stale) {
            endLiveService.endStaleLive(live);
            log.info("Auto-ended stale live {} (streamEndedAt={})", live.getId(), live.getStreamEndedAt());
        }
    }
}
