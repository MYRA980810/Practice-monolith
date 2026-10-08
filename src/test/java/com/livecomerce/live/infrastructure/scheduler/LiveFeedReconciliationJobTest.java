package com.livecomerce.live.infrastructure.scheduler;

import com.livecomerce.live.application.LiveFeedSyncService;
import com.livecomerce.live.application.port.out.LiveFeedPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveFeedReconciliationJobTest {

    private static final long RECONCILE_MS = 60_000L;

    @Mock LiveFeedSyncService liveFeedSyncService;
    @Mock LiveFeedPort        liveFeedPort;

    LiveFeedReconciliationJob sut;

    @BeforeEach
    void setUp() {
        sut = new LiveFeedReconciliationJob(liveFeedSyncService, liveFeedPort, RECONCILE_MS);
    }

    @Test
    void onStartup_rebuildsSnapshotIfMissing() {
        sut.rebuildSnapshotOnStartup();
        verify(liveFeedSyncService).rebuildIfMissing();
    }

    @Test
    void onStartup_failure_doesNotPreventStartup() {
        doThrow(new IllegalStateException("redis down")).when(liveFeedSyncService).rebuildIfMissing();
        assertThatCode(() -> sut.rebuildSnapshotOnStartup()).doesNotThrowAnyException();
    }

    @Test
    void everyCycle_lockAcquired_reconcilesSnapshotAgainstDatabase() {
        when(liveFeedPort.tryAcquireReconcileLock(Duration.ofMillis(54_000L))).thenReturn(true);

        sut.reconcileSnapshot();

        verify(liveFeedSyncService).reconcile();
    }

    @Test
    void everyCycle_lockHeldByAnotherReplica_skipsReconciliation() {
        when(liveFeedPort.tryAcquireReconcileLock(Duration.ofMillis(54_000L))).thenReturn(false);

        sut.reconcileSnapshot();

        verify(liveFeedSyncService, never()).reconcile();
    }

    @Test
    void everyCycle_failure_isSwallowedUntilNextCycle() {
        when(liveFeedPort.tryAcquireReconcileLock(Duration.ofMillis(54_000L))).thenReturn(true);
        doThrow(new IllegalStateException("redis down")).when(liveFeedSyncService).reconcile();
        assertThatCode(() -> sut.reconcileSnapshot()).doesNotThrowAnyException();
    }

    @Test
    void everyCycle_lockFailure_isSwallowedUntilNextCycle() {
        when(liveFeedPort.tryAcquireReconcileLock(Duration.ofMillis(54_000L)))
                .thenThrow(new IllegalStateException("redis down"));

        assertThatCode(() -> sut.reconcileSnapshot()).doesNotThrowAnyException();
        verify(liveFeedSyncService, never()).reconcile();
    }

    @Test
    void schedule_usesDedicatedReconcileInterval() throws Exception {
        var scheduled = LiveFeedReconciliationJob.class.getDeclaredMethod("reconcileSnapshot")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled.fixedDelayString()).isEqualTo("${live.feed.reconcile-ms:60000}");
    }
}
