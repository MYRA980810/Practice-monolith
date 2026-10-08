package com.livecomerce.live.infrastructure.scheduler;

import com.livecomerce.live.application.LiveFeedSyncService;
import com.livecomerce.live.application.port.out.LiveFeedPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Keeps the Redis feed snapshot honest: builds it on startup when it's missing, and on
 * every reconciliation cycle repairs drift against the database (missed or failed events,
 * a Redis flush). Every instance schedules the cycle, but a shared lease lets only one of
 * them reconcile per tick. Failures are logged and retried next cycle — the feed cache must
 * never block startup.
 */
@Component
class LiveFeedReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(LiveFeedReconciliationJob.class);

    private final LiveFeedSyncService liveFeedSyncService;
    private final LiveFeedPort        liveFeedPort;
    /** Slightly shorter than the interval, so the lease is free again by this instance's next tick. */
    private final Duration            lockTtl;

    LiveFeedReconciliationJob(LiveFeedSyncService liveFeedSyncService,
                              LiveFeedPort liveFeedPort,
                              @Value("${live.feed.reconcile-ms:60000}") long reconcileMs) {
        this.liveFeedSyncService = liveFeedSyncService;
        this.liveFeedPort        = liveFeedPort;
        this.lockTtl             = Duration.ofMillis(reconcileMs * 9 / 10);
    }

    @EventListener(ApplicationReadyEvent.class)
    void rebuildSnapshotOnStartup() {
        try {
            liveFeedSyncService.rebuildIfMissing();
        } catch (Exception e) {
            log.warn("Live feed snapshot rebuild on startup failed, retrying on next reconciliation: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${live.feed.reconcile-ms:60000}")
    void reconcileSnapshot() {
        try {
            if (!liveFeedPort.tryAcquireReconcileLock(lockTtl)) {
                log.debug("Live feed reconciliation skipped: another instance holds the lease");
                return;
            }
            liveFeedSyncService.reconcile();
        } catch (Exception e) {
            log.warn("Live feed snapshot reconciliation failed: {}", e.getMessage());
        }
    }
}
