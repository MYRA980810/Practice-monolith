package com.livecomerce.live.infrastructure;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records a failed room-event delivery: increments {@code live.room_events.failed}
 * (tags: provider, eventName) on every failure and logs at WARN at most once per
 * live per {@link #WARN_WINDOW}, the rest at DEBUG, so an outage cannot flood the logs.
 */
public class RoomEventFailureReporter {

    static final Duration WARN_WINDOW       = Duration.ofSeconds(60);
    static final int      MAX_TRACKED_LIVES = 10_000;

    private static final String METRIC = "live.room_events.failed";

    private final String                provider;
    private final Logger                log;
    private final MeterRegistry         meterRegistry;
    private final Clock                 clock;
    private final Map<UUID, Instant>    lastWarnAt = new ConcurrentHashMap<>();

    public RoomEventFailureReporter(String provider, Logger log, MeterRegistry meterRegistry, Clock clock) {
        this.provider      = provider;
        this.log           = log;
        this.meterRegistry = meterRegistry;
        this.clock         = clock;
    }

    public void report(UUID liveId, String eventName, Exception error) {
        Counter.builder(METRIC)
                .tag("provider", provider)
                .tag("eventName", eventName)
                .register(meterRegistry)
                .increment();

        String message = "Room event delivery failed: provider=" + provider + ", liveId={}, eventName={}, error={}: {}";
        String errorType = error.getClass().getSimpleName();
        if (claimWarnSlot(liveId)) {
            log.warn(message, liveId, eventName, errorType, error.getMessage());
        } else {
            log.debug(message, liveId, eventName, errorType, error.getMessage());
        }
    }

    int trackedLives() {
        return lastWarnAt.size();
    }

    private boolean claimWarnSlot(UUID liveId) {
        Instant now = clock.instant();
        if (lastWarnAt.size() >= MAX_TRACKED_LIVES) {
            lastWarnAt.values().removeIf(at -> !now.isBefore(at.plus(WARN_WINDOW)));
            if (lastWarnAt.size() >= MAX_TRACKED_LIVES) {
                lastWarnAt.clear();
            }
        }
        boolean[] claimed = {false};
        lastWarnAt.compute(liveId, (id, previous) -> {
            if (previous == null || !now.isBefore(previous.plus(WARN_WINDOW))) {
                claimed[0] = true;
                return now;
            }
            return previous;
        });
        return claimed[0];
    }
}
