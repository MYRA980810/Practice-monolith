package com.livecomerce.live.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.livecomerce.live.application.LiveFeedCard;
import com.livecomerce.live.application.LiveFeedEvent;
import com.livecomerce.live.application.port.out.LoadLivePort.CategoryLiveCount;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the adapter's Lua scripts against a real Redis: snapshot state, version seeding and
 * the JSON each write publishes on {@value RedisLiveFeedAdapter#CHANNEL}. Skipped without Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisLiveFeedAdapterIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    private static final long NOW_MILLIS = 1_760_000_000_000L;

    private static final UUID CATEGORY_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID CATEGORY_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    static LettuceConnectionFactory connectionFactory;
    static StringRedisTemplate      redisTemplate;

    final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();

    final BlockingQueue<String> published = new LinkedBlockingQueue<>();
    RedisMessageListenerContainer listenerContainer;
    RedisLiveFeedAdapter adapter;
    long nowMillis = NOW_MILLIS;

    @BeforeAll
    static void connect() {
        connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        redisTemplate = new StringRedisTemplate(connectionFactory);
    }

    @AfterAll
    static void disconnect() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() throws Exception {
        redisTemplate.execute(connection -> {
            connection.serverCommands().flushAll();
            return null;
        }, true);
        listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(connectionFactory);
        listenerContainer.addMessageListener(
                (message, pattern) -> published.add(new String(message.getBody(), StandardCharsets.UTF_8)),
                new ChannelTopic(RedisLiveFeedAdapter.CHANNEL));
        listenerContainer.afterPropertiesSet();
        listenerContainer.start(); // blocks until the subscription is registered
        adapter = new RedisLiveFeedAdapter(redisTemplate, objectMapper, () -> nowMillis);
    }

    @AfterEach
    void tearDown() throws Exception {
        listenerContainer.destroy();
    }

    private static LiveFeedCard card(UUID liveId, UUID categoryId, String startedAt) {
        return new LiveFeedCard(liveId, UUID.randomUUID(), null, "Live " + liveId, "Seller", null,
                42L, Instant.parse(startedAt), categoryId);
    }

    private String nextPublished() throws InterruptedException {
        var message = published.poll(5, TimeUnit.SECONDS);
        assertThat(message).as("message published on live:feed").isNotNull();
        return message;
    }

    @Test
    void upsert_onEmptyFeed_seedsVersion_storesCard_andPublishesLiveAdded() throws Exception {
        var live = card(UUID.randomUUID(), CATEGORY_A, "2026-10-06T10:00:00Z");

        var event = adapter.upsert(live);

        var stored = live.withCurrentViewers(0);
        var counts = List.of(new CategoryLiveCount(CATEGORY_A, 1));
        assertThat(event).isEqualTo(new LiveFeedEvent.Added(NOW_MILLIS, stored, counts));
        assertThat(nextPublished()).isEqualTo("{\"type\":\"live-added\",\"seq\":" + NOW_MILLIS
                + ",\"card\":" + objectMapper.writeValueAsString(stored)
                + ",\"counts\":[{\"categoryId\":\"" + CATEGORY_A + "\",\"count\":1}]}");
    }

    @Test
    void upsert_existingLiveWithNewCategory_movesCount_andKeepsIncrementing() throws Exception {
        var liveId = UUID.randomUUID();
        adapter.upsert(card(liveId, CATEGORY_A, "2026-10-06T10:00:00Z"));
        nextPublished();

        var event = (LiveFeedEvent.Added) adapter.upsert(card(liveId, CATEGORY_B, "2026-10-06T10:00:00Z"));

        assertThat(event.seq()).isEqualTo(NOW_MILLIS + 1);
        assertThat(event.counts()).containsExactly(new CategoryLiveCount(CATEGORY_B, 1));
        assertThat(objectMapper.readValue(nextPublished(), LiveFeedEvent.class)).isEqualTo(event);
        assertThat(adapter.snapshot().cards()).hasSize(1);
    }

    @Test
    void remove_presentLive_dropsCardAndZeroCount_andPublishesLiveRemoved() throws Exception {
        var liveId = UUID.randomUUID();
        adapter.upsert(card(liveId, CATEGORY_A, "2026-10-06T10:00:00Z"));
        nextPublished();

        var event = adapter.remove(liveId);

        assertThat(event).contains(new LiveFeedEvent.Removed(NOW_MILLIS + 1, liveId, List.of()));
        assertThat(nextPublished()).isEqualTo("{\"type\":\"live-removed\",\"seq\":" + (NOW_MILLIS + 1)
                + ",\"liveId\":\"" + liveId + "\",\"counts\":[]}");
        var snapshot = adapter.snapshot();
        assertThat(snapshot.cards()).isEmpty();
        assertThat(snapshot.counts()).isEmpty();
    }

    @Test
    void remove_absentLive_noVersionBump_nothingPublished() throws Exception {
        assertThat(adapter.remove(UUID.randomUUID())).isEmpty();

        assertThat(adapter.currentVersion()).isZero();
        assertThat(published.poll(300, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void replaceAll_rebuildsCardsAndCounts_seedsVersionFromClock_andPublishesResync() throws Exception {
        nowMillis = 5L;
        adapter.upsert(card(UUID.randomUUID(), CATEGORY_A, "2026-10-06T10:00:00Z")); // stale card
        nextPublished();
        nowMillis = NOW_MILLIS;
        var older = card(UUID.randomUUID(), CATEGORY_B, "2026-10-06T09:00:00Z");
        var newer = card(UUID.randomUUID(), null, "2026-10-06T11:00:00Z");

        var event = adapter.replaceAll(List.of(older, newer));

        assertThat(event).isEqualTo(new LiveFeedEvent.Resynced(NOW_MILLIS));
        assertThat(nextPublished()).isEqualTo("{\"type\":\"resync\",\"seq\":" + NOW_MILLIS + "}");
        var snapshot = adapter.snapshot();
        assertThat(snapshot.version()).isEqualTo(NOW_MILLIS);
        assertThat(snapshot.cards()).containsExactly(newer.withCurrentViewers(0), older.withCurrentViewers(0));
        assertThat(snapshot.counts()).containsExactly(new CategoryLiveCount(CATEGORY_B, 1));

        var next = (LiveFeedEvent.Added) adapter.upsert(card(UUID.randomUUID(), CATEGORY_B, "2026-10-06T12:00:00Z"));
        assertThat(next.seq()).isEqualTo(NOW_MILLIS + 1);
    }

    @Test
    void replaceAll_afterFlush_versionNeverRegresses() {
        adapter.replaceAll(List.of());
        adapter.upsert(card(UUID.randomUUID(), CATEGORY_A, "2026-10-06T10:00:00Z"));
        var before = adapter.currentVersion();
        redisTemplate.execute(connection -> {
            connection.serverCommands().flushAll();
            return null;
        }, true);
        nowMillis = NOW_MILLIS + 60_000L;

        var event = adapter.replaceAll(List.of());

        assertThat(((LiveFeedEvent.Resynced) event).seq()).isGreaterThan(before);
    }

    @Test
    void replaceAll_versionAheadOfClock_keepsIncrementing() {
        adapter.replaceAll(List.of());
        nowMillis = NOW_MILLIS - 5_000L; // another replica's clock lags behind

        var event = adapter.replaceAll(List.of());

        assertThat(event).isEqualTo(new LiveFeedEvent.Resynced(NOW_MILLIS + 1));
    }

    @Test
    void snapshot_onEmptyFeed_isVersionZeroAndEmpty() {
        var snapshot = adapter.snapshot();

        assertThat(snapshot.version()).isZero();
        assertThat(snapshot.cards()).isEmpty();
        assertThat(snapshot.counts()).isEmpty();
    }

    @Test
    void reconcileLock_onlyOneHolderUntilItExpires() throws Exception {
        var otherReplica = new RedisLiveFeedAdapter(redisTemplate, objectMapper, () -> nowMillis);

        assertThat(adapter.tryAcquireReconcileLock(Duration.ofMillis(300))).isTrue();
        assertThat(otherReplica.tryAcquireReconcileLock(Duration.ofMillis(300))).isFalse();

        Thread.sleep(500);
        assertThat(otherReplica.tryAcquireReconcileLock(Duration.ofMillis(300))).isTrue();
    }
}
