package com.livecomerce.live.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.livecomerce.live.application.LiveFeedCard;
import com.livecomerce.live.application.LiveFeedCardAssembler;
import com.livecomerce.live.application.LiveFeedEvent;
import com.livecomerce.live.application.LiveFeedSnapshot;
import com.livecomerce.live.application.port.out.LiveFeedPort;
import com.livecomerce.live.application.port.out.LoadLivePort.CategoryLiveCount;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveFeedEmitterRegistryTest {

    @Mock LiveFeedPort          liveFeedPort;
    @Mock LiveFeedCardAssembler assembler;

    final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
    final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    final Deque<RecordingEmitter> nextEmitters = new ArrayDeque<>();

    LiveFeedEmitterRegistry registry;

    private static final int  MAX_PER_USER = 2;
    private static final long HANDSHAKE_RETRY_MS        = 15_000L;
    private static final long HANDSHAKE_RETRY_JITTER_MS = 2_000L;
    private static final UUID CATEGORY_ID  = UUID.randomUUID();
    private static final LiveFeedCard CARD = new LiveFeedCard(UUID.randomUUID(), UUID.randomUUID(), null,
            "Live", "Jane", null, 0L, Instant.parse("2026-10-07T10:00:00Z"), CATEGORY_ID);
    private static final List<CategoryLiveCount> COUNTS = List.of(new CategoryLiveCount(CATEGORY_ID, 1));

    @BeforeEach
    void setUp() {
        registry = new LiveFeedEmitterRegistry(liveFeedPort, assembler, objectMapper, meterRegistry, executor,
                () -> nextEmitters.isEmpty() ? new RecordingEmitter() : nextEmitters.poll(), 20_000L, MAX_PER_USER,
                HANDSHAKE_RETRY_MS, () -> HANDSHAKE_RETRY_JITTER_MS);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    /** Waits until every fan-out task queued so far has run (the executor is single-threaded). */
    private void drain() throws Exception {
        executor.submit(() -> { }).get();
    }

    /** Opens a connection for a fresh user, so the per-user cap never interferes. */
    private RecordingEmitter connect(String lastEventId) {
        return (RecordingEmitter) registry.connect(UUID.randomUUID(), lastEventId).orElseThrow();
    }

    private RecordingEmitter connectUpToDate(long version) {
        when(liveFeedPort.currentVersion()).thenReturn(version);
        return connect(String.valueOf(version));
    }

    private RecordingEmitter failingWith(RuntimeException error) {
        var emitter = new RecordingEmitter();
        nextEmitters.add(emitter);
        connectUpToDate(5L);
        emitter.failure = error;
        return emitter;
    }

    private double connections() {
        return meterRegistry.get("live.feed.sse.connections").gauge().value();
    }

    private double failures() {
        return meterRegistry.get("live.feed.sse.send.failures").counter().count();
    }

    // --- connect handshake ---

    @Test
    void connect_withoutLastEventId_flushesCommentThenSendsSnapshotWithViewerCounts() throws Exception {
        var stored = new LiveFeedSnapshot(5L, List.of(CARD), COUNTS);
        var withViewers = new LiveFeedSnapshot(5L, List.of(CARD.withCurrentViewers(3)), COUNTS);
        when(liveFeedPort.currentVersion()).thenReturn(5L);
        when(liveFeedPort.snapshot()).thenReturn(stored);
        when(assembler.withViewerCounts(stored)).thenReturn(withViewers);

        var emitter = connect(null);

        assertThat(emitter.frames).containsExactly(
                ":connected\n\n",
                "id:5\nevent:snapshot\ndata:" + objectMapper.writeValueAsString(withViewers) + "\n\n");
        assertThat(connections()).isEqualTo(1.0);
    }

    @Test
    void connect_lastEventIdEqualToCurrentVersion_sendsNoSnapshot() {
        var emitter = connectUpToDate(5L);

        assertThat(emitter.frames).containsExactly(":connected\n\n");
        verify(liveFeedPort, never()).snapshot();
    }

    @Test
    void connect_staleAheadOrMalformedLastEventId_sendsSnapshot() {
        var snapshot = new LiveFeedSnapshot(5L, List.of(), List.of());
        when(liveFeedPort.currentVersion()).thenReturn(5L);
        when(liveFeedPort.snapshot()).thenReturn(snapshot);
        when(assembler.withViewerCounts(snapshot)).thenReturn(snapshot);

        for (var lastEventId : List.of("4", "6", "abc")) {
            var emitter = connect(lastEventId);
            assertThat(emitter.frames).as(lastEventId).anyMatch(frame -> frame.contains("event:snapshot"));
        }
    }

    @Test
    void connect_feedUnavailable_sendsResyncWithJitteredRetryAndClosesInsteadOfFailing() throws Exception {
        when(liveFeedPort.currentVersion()).thenThrow(new QueryTimeoutException("redis down"));

        var emitter = connect(null);

        // retry: base + injected jitter, so clients back off instead of looping while the feed is down.
        assertThat(emitter.frames).containsExactly(
                ":connected\n\n",
                "retry:17000\nevent:resync\ndata:" + objectMapper.writeValueAsString(new LiveFeedEvent.Resynced(0L)) + "\n\n");
        assertThat(emitter.completed).isTrue();
        assertThat(connections()).isZero();
        assertThat(meterRegistry.get("live.feed.sse.handshake.failures").counter().count()).isEqualTo(1.0);
    }

    @Test
    void connect_eventsDeliveredDuringHandshake_areSentAfterSnapshot_skippingThoseItCovers() throws Exception {
        var older = new LiveFeedEvent.Removed(4L, UUID.randomUUID(), List.of());
        var newer = new LiveFeedEvent.Added(6L, CARD, COUNTS);
        var snapshot = new LiveFeedSnapshot(5L, List.of(), List.of());
        when(liveFeedPort.currentVersion()).thenReturn(3L);
        when(liveFeedPort.snapshot()).thenReturn(snapshot);
        when(assembler.withViewerCounts(snapshot)).thenAnswer(invocation -> {
            registry.deliver(older);
            registry.deliver(newer);
            drain();
            return snapshot;
        });

        var emitter = connect(null);
        drain();

        assertThat(emitter.frames).hasSize(3);
        assertThat(emitter.frames.get(1)).startsWith("id:5\nevent:snapshot\n");
        assertThat(emitter.frames.get(2)).startsWith("id:6\nevent:live-added\n");
    }

    // --- per-user cap ---

    @Test
    void connect_overPerUserCap_isRejected_andCompletionReleasesTheSlot() {
        var user = UUID.randomUUID();
        when(liveFeedPort.currentVersion()).thenReturn(5L);
        var first = (RecordingEmitter) registry.connect(user, "5").orElseThrow();
        registry.connect(user, "5").orElseThrow();

        assertThat(registry.connect(user, "5")).isEmpty();
        assertThat(registry.connect(UUID.randomUUID(), "5")).isPresent();

        first.completionCallbacks.forEach(Runnable::run);

        assertThat(registry.connect(user, "5")).isPresent();
    }

    @Test
    void connect_slotIsReleasedOnTimeoutErrorAndDrop() throws Exception {
        var user = UUID.randomUUID();
        when(liveFeedPort.currentVersion()).thenReturn(5L);
        var timedOut = (RecordingEmitter) registry.connect(user, "5").orElseThrow();
        var errored  = (RecordingEmitter) registry.connect(user, "5").orElseThrow();

        timedOut.timeoutCallbacks.forEach(Runnable::run);
        errored.errorCallbacks.forEach(callback -> callback.accept(new IOException("reset")));
        var dead = new RecordingEmitter();
        nextEmitters.add(dead);
        registry.connect(user, "5").orElseThrow();
        registry.connect(user, "5").orElseThrow();
        dead.failure = new IllegalStateException("closed");
        registry.ping();

        assertThat(registry.connect(user, "5")).isPresent();
    }

    // --- fan-out ---

    @Test
    void deliver_sendsEventToEveryConnection_withSeqIdTypeNameAndEventJson() throws Exception {
        var first  = connectUpToDate(5L);
        var second = connect("5");
        var event  = new LiveFeedEvent.Added(6L, CARD, COUNTS);

        registry.deliver(event);
        drain();

        var expected = "id:6\nevent:live-added\ndata:" + objectMapper.writeValueAsString(event) + "\n\n";
        assertThat(first.frames).last().isEqualTo(expected);
        assertThat(second.frames).last().isEqualTo(expected);
        assertThat(meterRegistry.get("live.feed.sse.events.sent").counter().count()).isEqualTo(2.0);
        assertThat(meterRegistry.get("live.feed.sse.broadcast").timer().count()).isEqualTo(1L);
    }

    @Test
    void deliver_removedEvent_usesLiveRemovedName() throws Exception {
        var emitter = connectUpToDate(5L);

        registry.deliver(new LiveFeedEvent.Removed(6L, CARD.id(), COUNTS));
        drain();

        assertThat(emitter.frames).last().asString().startsWith("id:6\nevent:live-removed\ndata:{");
    }

    @Test
    void deliver_resync_isSentWithoutIdThenClosesTheConnection_droppingLaterEvents() throws Exception {
        var emitter = connectUpToDate(5L);
        var resync = new LiveFeedEvent.Resynced(9L);

        registry.deliver(resync);
        registry.deliver(new LiveFeedEvent.Added(10L, CARD, COUNTS));
        drain();

        assertThat(emitter.frames).containsExactly(
                ":connected\n\n",
                "event:resync\ndata:" + objectMapper.writeValueAsString(resync) + "\n\n");
        assertThat(emitter.completed).isTrue();
        assertThat(connections()).isZero();
    }

    @Test
    void deliver_resync_clientReconnectingWithItsLastSeenIdGetsASnapshot() throws Exception {
        connectUpToDate(5L);
        registry.deliver(new LiveFeedEvent.Resynced(9L));
        registry.deliver(new LiveFeedEvent.Added(10L, CARD, COUNTS));
        drain();
        var snapshot = new LiveFeedSnapshot(10L, List.of(CARD), COUNTS);
        when(liveFeedPort.currentVersion()).thenReturn(10L);
        when(liveFeedPort.snapshot()).thenReturn(snapshot);
        when(assembler.withViewerCounts(snapshot)).thenReturn(snapshot);

        // The last id-bearing frame the client saw was the handshake version, 5.
        var reconnected = connect("5");

        assertThat(reconnected.frames).last().asString().startsWith("id:10\nevent:snapshot\n");
    }

    @Test
    void deliver_eventAlreadyCoveredByConnectionVersion_isSkipped() throws Exception {
        var emitter = connectUpToDate(5L);

        registry.deliver(new LiveFeedEvent.Removed(5L, CARD.id(), COUNTS));
        drain();

        assertThat(emitter.frames).containsExactly(":connected\n\n");
    }

    @Test
    void deliver_sendFailure_removesConnectionAndCountsIt() throws Exception {
        var broken = new RecordingEmitter();
        nextEmitters.add(broken);
        connectUpToDate(5L);
        broken.failing = true;

        registry.deliver(new LiveFeedEvent.Added(6L, CARD, COUNTS));
        registry.deliver(new LiveFeedEvent.Added(7L, CARD, COUNTS));
        drain();

        assertThat(connections()).isZero();
        assertThat(broken.attempts).isEqualTo(2);
        assertThat(failures()).isEqualTo(1.0);
    }

    @Test
    void deliver_unexpectedErrorOnOneConnection_dropsOnlyThatOne() throws Exception {
        failingWith(new IllegalArgumentException("unexpected"));
        var healthy = connect("5");

        registry.deliver(new LiveFeedEvent.Added(6L, CARD, COUNTS));
        drain();

        assertThat(healthy.frames).last().asString().startsWith("id:6\n");
        assertThat(connections()).isEqualTo(1.0);
        assertThat(failures()).isEqualTo(1.0);
    }

    @Test
    void deliver_queuedTasks_areExposedAsGauge() throws Exception {
        var release = new CountDownLatch(1);
        executor.submit(() -> { release.await(); return null; });

        registry.deliver(new LiveFeedEvent.Resynced(1L));

        assertThat(meterRegistry.get("live.feed.sse.executor.queue").gauge().value()).isEqualTo(1.0);
        release.countDown();
        drain();
        assertThat(meterRegistry.get("live.feed.sse.executor.queue").gauge().value()).isZero();
    }

    @Test
    void deliver_afterShutdown_isIgnored() {
        registry.shutdown();

        assertThatCode(() -> registry.deliver(new LiveFeedEvent.Resynced(1L))).doesNotThrowAnyException();
        assertThat(meterRegistry.get("live.feed.sse.executor.queue").gauge().value()).isZero();
    }

    // --- heartbeat ---

    @Test
    void ping_sendsCommentToEveryConnection() {
        var emitter = connectUpToDate(5L);

        registry.ping();

        assertThat(emitter.frames).last().isEqualTo(":ping\n\n");
    }

    @Test
    void ping_failure_removesDeadConnection() {
        var dead = new RecordingEmitter();
        nextEmitters.add(dead);
        connectUpToDate(5L);
        dead.failing = true;

        registry.ping();

        assertThat(connections()).isZero();
        assertThat(failures()).isEqualTo(1.0);
    }

    @Test
    void ping_unexpectedErrorOnOneConnection_neitherThrowsNorSkipsTheOthers() {
        failingWith(new IllegalArgumentException("unexpected"));
        var healthy = connect("5");

        assertThatCode(registry::ping).doesNotThrowAnyException();

        assertThat(healthy.frames).last().isEqualTo(":ping\n\n");
        assertThat(connections()).isEqualTo(1.0);
    }

    // --- lifecycle callbacks ---

    @Test
    void completionTimeoutAndError_removeTheConnection() {
        var completed = connectUpToDate(5L);
        var timedOut  = connect("5");
        var errored   = connect("5");
        assertThat(connections()).isEqualTo(3.0);

        completed.completionCallbacks.forEach(Runnable::run);
        timedOut.timeoutCallbacks.forEach(Runnable::run);
        errored.errorCallbacks.forEach(callback -> callback.accept(new IOException("reset")));

        assertThat(connections()).isZero();
        assertThat(timedOut.completed).isTrue();
    }

    @Test
    void shutdown_completesEveryConnection() {
        var emitter = connectUpToDate(5L);

        registry.shutdown();

        assertThat(emitter.completed).isTrue();
        assertThat(connections()).isZero();
    }

    /** Records every frame as SSE text instead of writing it; can be switched to fail like a dead socket. */
    static class RecordingEmitter extends SseEmitter {

        final List<String> frames = new ArrayList<>();
        final List<Runnable> completionCallbacks = new ArrayList<>();
        final List<Runnable> timeoutCallbacks    = new ArrayList<>();
        final List<Consumer<Throwable>> errorCallbacks = new ArrayList<>();
        volatile boolean failing;
        volatile RuntimeException failure;
        volatile boolean completed;
        int attempts;

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            attempts++;
            if (failing) throw new IOException("Broken pipe");
            if (failure != null) throw failure;
            frames.add(builder.build().stream().map(part -> part.getData().toString()).collect(Collectors.joining()));
        }

        @Override
        public synchronized void onCompletion(Runnable callback) {
            completionCallbacks.add(callback);
        }

        @Override
        public synchronized void onTimeout(Runnable callback) {
            timeoutCallbacks.add(callback);
        }

        @Override
        public synchronized void onError(Consumer<Throwable> callback) {
            errorCallbacks.add(callback);
        }

        @Override
        public synchronized void complete() {
            completed = true;
        }
    }
}
