package com.livecomerce.live.infrastructure;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RoomEventFailureReporterTest {

    private static final UUID      LIVE_ID = UUID.randomUUID();
    private static final Instant   T0      = Instant.parse("2026-11-22T10:00:00Z");

    private final Logger             log      = mock(Logger.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MutableClock       clock    = new MutableClock(T0);
    private final RoomEventFailureReporter sut = new RoomEventFailureReporter("ivs", log, registry, clock);

    @Test
    void firstFailureOfALive_logsWarnWithLiveIdEventNameAndExceptionClass() {
        sut.report(LIVE_ID, "viewer-count", new IllegalStateException("boom"));

        verify(log).warn(anyString(), eq(LIVE_ID), eq("viewer-count"), eq("IllegalStateException"), eq("boom"));
        verify(log, never()).debug(anyString(), any(), any(), any(), any());
    }

    @Test
    void repeatedFailureWithinWindow_logsDebug() {
        sut.report(LIVE_ID, "viewer-count", new IllegalStateException("boom"));
        clock.advance(Duration.ofSeconds(59));
        sut.report(LIVE_ID, "viewer-count", new IllegalStateException("boom"));

        verify(log, times(1)).warn(anyString(), any(), any(), any(), any());
        verify(log, times(1)).debug(anyString(), any(), any(), any(), any());
    }

    @Test
    void failureAfterWindow_logsWarnAgain() {
        sut.report(LIVE_ID, "viewer-count", new IllegalStateException("boom"));
        clock.advance(Duration.ofSeconds(60));
        sut.report(LIVE_ID, "viewer-count", new IllegalStateException("boom"));

        verify(log, times(2)).warn(anyString(), any(), any(), any(), any());
    }

    @Test
    void differentLives_areThrottledIndependently() {
        sut.report(LIVE_ID, "viewer-count", new IllegalStateException("boom"));
        sut.report(UUID.randomUUID(), "viewer-count", new IllegalStateException("boom"));

        verify(log, times(2)).warn(anyString(), any(), any(), any(), any());
    }

    @Test
    void everyFailure_incrementsCounterTaggedByProviderAndEventName() {
        sut.report(LIVE_ID, "viewer-count", new IllegalStateException("boom"));
        sut.report(LIVE_ID, "viewer-count", new IllegalStateException("boom"));

        var counter = registry.find("live.room_events.failed")
                .tag("provider", "ivs").tag("eventName", "viewer-count").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    void trackedLives_areBounded() {
        for (int i = 0; i < RoomEventFailureReporter.MAX_TRACKED_LIVES + 10; i++) {
            sut.report(UUID.randomUUID(), "viewer-count", new IllegalStateException("boom"));
        }

        assertThat(sut.trackedLives()).isLessThanOrEqualTo(RoomEventFailureReporter.MAX_TRACKED_LIVES);
    }

    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) { this.now = now; }

        void advance(Duration duration) { now = now.plus(duration); }

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
