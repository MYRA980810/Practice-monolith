package com.livecomerce.live.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.LiveFeedCardAssembler;
import com.livecomerce.live.application.LiveFeedEvent;
import com.livecomerce.live.application.port.in.LiveFeedEventSink;
import com.livecomerce.live.application.port.out.LiveFeedPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Open connections of the live feed SSE stream on this instance. Every {@link LiveFeedEvent}
 * is fanned out to all of them as {@code id: <seq>}, {@code event: <type>}, {@code data: <event
 * JSON>} (the same JSON as the Pub/Sub payload).
 *
 * <p>A {@code resync} carries no {@code id} and the server closes the connection right after
 * it, dropping any later event for it: the client reconnects with the last seq it actually
 * applied, which is now behind the current version, so it gets a fresh snapshot. (Leaving the
 * connection open would let a later id-bearing event advance its Last-Event-ID past the lost
 * changes, and its reconnect would then skip the snapshot.) A handshake that can't read the feed
 * (e.g. Redis down) ends the same way, after {@code :connected}, instead of failing the request;
 * only that resync also sets {@code retry:} ({@code live.feed.sse.handshake-retry-ms} plus random
 * jitter) so clients back off while the feed is down instead of reconnecting in a tight loop.
 *
 * <p>Fan-out and the {@code : ping} heartbeat run on one dedicated single-thread executor (not
 * the shared task scheduler), so {@link #deliver} never blocks the Pub/Sub listener thread and
 * events reach each connection in {@code seq} order. Any failure writing to a connection — event
 * or ping, I/O or unexpected — drops only that connection, which doubles as the dead-connection
 * sweep. Residual risk: a slow client's blocking write delays everyone behind it (head-of-line);
 * virtual threads ({@code live.feed.sse.virtual-threads}) don't change that, they only make the
 * executor thread virtual. {@code live.feed.sse.broadcast} and {@code live.feed.sse.executor.queue}
 * show when that happens.
 *
 * <p>Each user may hold at most {@code live.feed.sse.max-connections-per-user} connections on
 * this instance; further ones are refused (never evicting the open ones).
 */
@Component
class LiveFeedEmitterRegistry implements LiveFeedEventSink {

    static final String SNAPSHOT_EVENT = "snapshot";

    private static final Logger log = LoggerFactory.getLogger(LiveFeedEmitterRegistry.class);

    private final LiveFeedPort             liveFeedPort;
    private final LiveFeedCardAssembler    assembler;
    private final ObjectMapper             objectMapper;
    private final ScheduledExecutorService executor;
    private final Supplier<SseEmitter>     emitterFactory;
    private final long                     pingMs;
    private final int                      maxConnectionsPerUser;
    private final long                     handshakeRetryMs;
    private final LongSupplier             handshakeRetryJitter;
    private final Set<Connection>          connections        = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer>       connectionsPerUser = new ConcurrentHashMap<>();
    private final AtomicInteger            queuedBroadcasts   = new AtomicInteger();
    private final Counter                  eventsSent;
    private final Counter                  sendFailures;
    private final Counter                  handshakeFailures;
    private final Timer                    broadcastTimer;

    @Autowired
    LiveFeedEmitterRegistry(LiveFeedPort liveFeedPort,
                            LiveFeedCardAssembler assembler,
                            ObjectMapper objectMapper,
                            MeterRegistry meterRegistry,
                            @Value("${live.feed.sse.timeout-ms:840000}") long timeoutMs,
                            @Value("${live.feed.sse.ping-ms:20000}") long pingMs,
                            @Value("${live.feed.sse.virtual-threads:false}") boolean virtualThreads,
                            @Value("${live.feed.sse.max-connections-per-user:5}") int maxConnectionsPerUser,
                            @Value("${live.feed.sse.handshake-retry-ms:15000}") long handshakeRetryMs,
                            @Value("${live.feed.sse.handshake-retry-jitter-ms:5000}") long handshakeRetryJitterMs) {
        this(liveFeedPort, assembler, objectMapper, meterRegistry, newExecutor(virtualThreads),
                () -> new SseEmitter(timeoutMs), pingMs, maxConnectionsPerUser, handshakeRetryMs,
                () -> ThreadLocalRandom.current().nextLong(Math.max(handshakeRetryJitterMs, 0L) + 1));
    }

    LiveFeedEmitterRegistry(LiveFeedPort liveFeedPort,
                            LiveFeedCardAssembler assembler,
                            ObjectMapper objectMapper,
                            MeterRegistry meterRegistry,
                            ScheduledExecutorService executor,
                            Supplier<SseEmitter> emitterFactory,
                            long pingMs,
                            int maxConnectionsPerUser,
                            long handshakeRetryMs,
                            LongSupplier handshakeRetryJitter) {
        this.liveFeedPort          = liveFeedPort;
        this.assembler             = assembler;
        this.objectMapper          = objectMapper;
        this.executor              = executor;
        this.emitterFactory        = emitterFactory;
        this.pingMs                = pingMs;
        this.maxConnectionsPerUser = maxConnectionsPerUser;
        this.handshakeRetryMs      = handshakeRetryMs;
        this.handshakeRetryJitter  = handshakeRetryJitter;
        Gauge.builder("live.feed.sse.connections", connections, Set::size)
                .description("Open live feed SSE connections on this instance")
                .register(meterRegistry);
        Gauge.builder("live.feed.sse.executor.queue", queuedBroadcasts, AtomicInteger::get)
                .description("Live feed fan-out tasks waiting for the SSE executor")
                .register(meterRegistry);
        this.eventsSent = Counter.builder("live.feed.sse.events.sent")
                .description("Live feed events written to SSE connections")
                .register(meterRegistry);
        this.sendFailures = Counter.builder("live.feed.sse.send.failures")
                .description("Live feed SSE connections dropped after a failed send or ping")
                .register(meterRegistry);
        this.handshakeFailures = Counter.builder("live.feed.sse.handshake.failures")
                .description("Live feed SSE connections closed with a resync because the feed was unreadable")
                .register(meterRegistry);
        this.broadcastTimer = Timer.builder("live.feed.sse.broadcast")
                .description("Time to fan one live feed event out to every SSE connection")
                .register(meterRegistry);
    }

    private static ScheduledExecutorService newExecutor(boolean virtualThreads) {
        var threads = virtualThreads
                ? Thread.ofVirtual().name("live-feed-sse").factory()
                : Thread.ofPlatform().name("live-feed-sse").daemon(true).factory();
        return Executors.newSingleThreadScheduledExecutor(threads);
    }

    @PostConstruct
    void startHeartbeat() {
        // A periodic task that throws is never run again: ping() must not, but guard anyway.
        executor.scheduleAtFixedRate(() -> {
            try {
                ping();
            } catch (RuntimeException e) {
                log.warn("Live feed SSE ping round failed: {}", e.getMessage());
            }
        }, pingMs, pingMs, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
        connections.forEach(connection -> {
            remove(connection);
            connection.emitter.complete();
        });
    }

    /**
     * Opens a connection for {@code userId}; empty when the user is already at the per-instance
     * cap. The connection is registered <em>before</em> the version is read, and events delivered
     * until the handshake ends are queued on it, so none published meanwhile is lost. Then: if
     * {@code lastEventId} is exactly the current version the client is up to date and gets
     * nothing extra; otherwise (absent, older, ahead or malformed — there's no event history to
     * replay) it gets a {@code snapshot} event. Queued and later events at or below the version
     * the client now holds are skipped, so it never sees a duplicate.
     */
    Optional<SseEmitter> connect(UUID userId, @Nullable String lastEventId) {
        if (!reserveSlot(userId)) return Optional.empty();
        var connection = new Connection(userId, emitterFactory.get());
        var emitter = connection.emitter;
        emitter.onCompletion(() -> remove(connection));
        emitter.onTimeout(() -> {
            remove(connection);
            emitter.complete();
        });
        emitter.onError(error -> remove(connection));
        connections.add(connection);
        try {
            // First bytes right away: Railway's edge drops a request that sends nothing for 60 s.
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            remove(connection);
            throw new UncheckedIOException(e);
        }
        try {
            long version = liveFeedPort.currentVersion();
            if (isUpToDate(lastEventId, version)) {
                connection.open(version, null);
            } else {
                var snapshot = assembler.withViewerCounts(liveFeedPort.snapshot());
                connection.open(snapshot.version(), SseEmitter.event()
                        .id(String.valueOf(snapshot.version())).name(SNAPSHOT_EVENT).data(toJson(snapshot)));
            }
        } catch (RuntimeException e) {
            handshakeFailures.increment();
            log.warn("Live feed unavailable on SSE connect, sending resync: {}", e.getMessage());
            var resync = new LiveFeedEvent.Resynced(0L);
            connection.resyncAndClose(SseEmitter.event()
                    .reconnectTime(handshakeRetryMs + handshakeRetryJitter.getAsLong())
                    .name(LiveFeedEvent.Resynced.TYPE).data(toJson(resync)));
        }
        return Optional.of(emitter);
    }

    @Override
    public void deliver(LiveFeedEvent event) {
        queuedBroadcasts.incrementAndGet();
        try {
            executor.execute(() -> {
                queuedBroadcasts.decrementAndGet();
                broadcastTimer.record(() -> broadcast(event));
            });
        } catch (RejectedExecutionException e) {
            queuedBroadcasts.decrementAndGet();
            log.debug("Live feed event seq {} not sent: SSE executor is shut down", event.seq());
        }
    }

    /** Heartbeat: keeps proxies from idling the connection out and drops the dead ones. */
    void ping() {
        connections.forEach(connection -> {
            try {
                connection.trySend(SseEmitter.event().comment("ping"));
            } catch (RuntimeException e) {
                drop(connection, e);
            }
        });
    }

    private void broadcast(LiveFeedEvent event) {
        String json;
        try {
            json = toJson(event);
        } catch (IllegalStateException e) {
            log.warn("Live feed event seq {} not sent: {}", event.seq(), e.getMessage());
            return;
        }
        var outgoing = new Outgoing(event, json);
        connections.forEach(connection -> {
            try {
                connection.deliver(outgoing);
            } catch (RuntimeException e) {
                drop(connection, e);
            }
        });
    }

    private boolean reserveSlot(UUID userId) {
        var reserved = new boolean[1];
        connectionsPerUser.compute(userId, (id, open) -> {
            int count = open == null ? 0 : open;
            if (count >= maxConnectionsPerUser) return open;
            reserved[0] = true;
            return count + 1;
        });
        return reserved[0];
    }

    /** Unregisters the connection and frees its user's slot; idempotent. */
    private boolean remove(Connection connection) {
        if (!connections.remove(connection)) return false;
        connectionsPerUser.computeIfPresent(connection.userId, (id, open) -> open > 1 ? open - 1 : null);
        return true;
    }

    private void drop(Connection connection, Exception cause) {
        if (remove(connection)) {
            sendFailures.increment();
            log.debug("Dropping live feed SSE connection: {}", cause.getMessage());
        }
    }

    private static boolean isUpToDate(@Nullable String lastEventId, long version) {
        if (lastEventId == null) return false;
        try {
            return Long.parseLong(lastEventId.trim()) == version;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize live feed payload", e);
        }
    }

    /** An event with its JSON serialized once for every connection. */
    private record Outgoing(LiveFeedEvent event, String json) {

        boolean isResync() {
            return event instanceof LiveFeedEvent.Resynced;
        }

        SseEventBuilder toSse() {
            var builder = SseEmitter.event();
            if (!isResync()) builder.id(String.valueOf(event.seq()));
            return builder.name(typeOf(event)).data(json);
        }

        private static String typeOf(LiveFeedEvent event) {
            return switch (event) {
                case LiveFeedEvent.Added added       -> LiveFeedEvent.Added.TYPE;
                case LiveFeedEvent.Removed removed   -> LiveFeedEvent.Removed.TYPE;
                case LiveFeedEvent.Resynced resynced -> LiveFeedEvent.Resynced.TYPE;
            };
        }
    }

    /** One client. Every write holds {@link #lock}, so frames never interleave. */
    private final class Connection {

        final UUID userId;
        final SseEmitter emitter;
        private final ReentrantLock lock = new ReentrantLock();
        /** Events delivered before the handshake settled the client's version; {@code null} once open. */
        private List<Outgoing> pending = new ArrayList<>();
        /** Version the client holds: events with {@code seq} at or below it are duplicates. */
        private long version;
        /** Set once a resync closed the connection: nothing else is written to it. */
        private boolean closed;

        Connection(UUID userId, SseEmitter emitter) {
            this.userId  = userId;
            this.emitter = emitter;
        }

        void open(long version, @Nullable SseEventBuilder snapshot) {
            lock.lock();
            try {
                this.version = version;
                var queued = pending;
                pending = null;
                if (snapshot != null && !trySend(snapshot)) return;
                for (var outgoing : queued) {
                    if (!forward(outgoing)) return;
                }
            } finally {
                lock.unlock();
            }
        }

        void deliver(Outgoing outgoing) {
            lock.lock();
            try {
                if (pending != null) {
                    pending.add(outgoing);
                } else {
                    forward(outgoing);
                }
            } finally {
                lock.unlock();
            }
        }

        /** Sends a resync, then closes the connection so the client reconnects for a snapshot. */
        void resyncAndClose(SseEventBuilder resync) {
            lock.lock();
            try {
                pending = null;
                if (!closed && trySend(resync)) eventsSent.increment();
                closed = true;
            } finally {
                lock.unlock();
            }
            remove(this);
            emitter.complete();
        }

        /** False when nothing more may be written (dropped, or closed after a resync). */
        private boolean forward(Outgoing outgoing) {
            if (closed) return false;
            if (outgoing.isResync()) {
                resyncAndClose(outgoing.toSse());
                return false;
            }
            if (outgoing.event().seq() <= version) return true;
            if (!trySend(outgoing.toSse())) return false;
            eventsSent.increment();
            return true;
        }

        /** False when the send failed and the connection was dropped. */
        boolean trySend(SseEventBuilder frame) {
            lock.lock();
            try {
                if (closed) return false;
                emitter.send(frame);
                return true;
            } catch (IOException | RuntimeException e) {
                // No completeWithError: after a failed send the container reports the error itself.
                drop(this, e);
                return false;
            } finally {
                lock.unlock();
            }
        }
    }
}
