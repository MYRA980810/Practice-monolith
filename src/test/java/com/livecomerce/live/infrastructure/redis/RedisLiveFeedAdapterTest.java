package com.livecomerce.live.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.livecomerce.live.application.LiveFeedCard;
import com.livecomerce.live.application.LiveFeedEvent;
import com.livecomerce.live.application.port.out.LoadLivePort.CategoryLiveCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLiveFeedAdapterTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    /** Configured like Spring Boot's auto-configured mapper (ISO-8601 dates), which the app injects. */
    final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();

    RedisLiveFeedAdapter adapter;

    private static final List<String> KEYS = List.of(
            "live:feed:cards", "live:feed:card-category", "live:feed:counts", "live:feed:version");
    private static final String CHANNEL = "live:feed";
    private static final long NOW_MILLIS = 1_760_000_000_000L;
    private static final String NOW = String.valueOf(NOW_MILLIS);

    private static final UUID LIVE_ID     = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SELLER_ID   = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CATEGORY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    /** Captures the single script invocation and replies with {@code reply}. */
    private final class ScriptCall {
        RedisScript<?> script;
        List<String> keys;
        List<Object> args;

        ScriptCall replying(String reply) {
            doAnswer(invocation -> {
                Object[] all = invocation.getArguments();
                script = invocation.getArgument(0);
                keys   = invocation.getArgument(1);
                args   = Arrays.asList(all).subList(2, all.length);
                return reply;
            }).when(redisTemplate).execute(any(RedisScript.class), anyList(), any(Object[].class));
            return this;
        }
    }

    @BeforeEach
    void setUp() {
        adapter = new RedisLiveFeedAdapter(redisTemplate, objectMapper, () -> NOW_MILLIS);
    }

    private static LiveFeedCard card(UUID categoryId) {
        return new LiveFeedCard(LIVE_ID, SELLER_ID, null, "My Live", "Seller", "https://img/x.png",
                99L, Instant.parse("2026-10-06T10:00:00Z"), categoryId);
    }

    private static final String COUNTS_JSON = "[{\"categoryId\":\"" + CATEGORY_ID + "\",\"count\":3}]";

    @Test
    void upsert_runsUpsertScriptWithCardJsonWithoutViewers_andParsesReturnedEvent() throws Exception {
        var expectedCard = card(CATEGORY_ID).withCurrentViewers(0);
        var cardJson = objectMapper.writeValueAsString(expectedCard);
        var call = new ScriptCall().replying(
                "{\"type\":\"live-added\",\"seq\":12,\"card\":" + cardJson + ",\"counts\":" + COUNTS_JSON + "}");

        var event = adapter.upsert(card(CATEGORY_ID));

        assertThat(call.script).isSameAs(RedisLiveFeedAdapter.UPSERT_SCRIPT);
        assertThat(call.keys).isEqualTo(KEYS);
        assertThat(call.args).containsExactly(CHANNEL, NOW, LIVE_ID.toString(), CATEGORY_ID.toString(), cardJson);
        assertThat(event).isEqualTo(new LiveFeedEvent.Added(12L, expectedCard,
                List.of(new CategoryLiveCount(CATEGORY_ID, 3))));
    }

    @Test
    void upsert_uncategorizedLive_passesEmptyCategory() {
        var call = new ScriptCall().replying("{\"type\":\"live-added\",\"seq\":1,\"card\":"
                + "{\"id\":\"" + LIVE_ID + "\",\"sellerId\":\"" + SELLER_ID + "\",\"title\":\"t\",\"currentViewers\":0,"
                + "\"startedAt\":\"2026-10-06T10:00:00Z\"},\"counts\":[]}");

        adapter.upsert(card(null));

        assertThat(call.args.get(3)).isEqualTo("");
    }

    @Test
    void cardJson_usesInjectedMapper_soInstantsMatchRestResponses() throws Exception {
        var call = new ScriptCall().replying("{\"type\":\"live-added\",\"seq\":1,\"card\":"
                + objectMapper.writeValueAsString(card(CATEGORY_ID).withCurrentViewers(0)) + ",\"counts\":[]}");

        adapter.upsert(card(CATEGORY_ID));

        assertThat((String) call.args.get(4)).contains("\"startedAt\":\"2026-10-06T10:00:00Z\"");
    }

    @Test
    void remove_presentLive_runsRemoveScript_andParsesEvent() {
        var call = new ScriptCall().replying(
                "{\"type\":\"live-removed\",\"seq\":13,\"liveId\":\"" + LIVE_ID + "\",\"counts\":" + COUNTS_JSON + "}");

        var event = adapter.remove(LIVE_ID);

        assertThat(call.script).isSameAs(RedisLiveFeedAdapter.REMOVE_SCRIPT);
        assertThat(call.keys).isEqualTo(KEYS);
        assertThat(call.args).containsExactly(CHANNEL, NOW, LIVE_ID.toString());
        assertThat(event).contains(new LiveFeedEvent.Removed(13L, LIVE_ID,
                List.of(new CategoryLiveCount(CATEGORY_ID, 3))));
    }

    @Test
    void remove_absentLive_scriptReturnsNil_isEmpty() {
        new ScriptCall().replying(null);

        assertThat(adapter.remove(LIVE_ID)).isEmpty();
    }

    @Test
    void replaceAll_passesLiveIdCategoryCardTriples_andParsesResync() throws Exception {
        var other = new LiveFeedCard(UUID.randomUUID(), SELLER_ID, null, "Other", null, null, 5L,
                Instant.parse("2026-10-06T11:00:00Z"), null);
        var call = new ScriptCall().replying("{\"type\":\"resync\",\"seq\":40}");

        var event = adapter.replaceAll(List.of(card(CATEGORY_ID), other));

        assertThat(call.script).isSameAs(RedisLiveFeedAdapter.REPLACE_ALL_SCRIPT);
        assertThat(call.keys).isEqualTo(KEYS);
        assertThat(call.args).containsExactly(
                CHANNEL, NOW,
                LIVE_ID.toString(), CATEGORY_ID.toString(),
                objectMapper.writeValueAsString(card(CATEGORY_ID).withCurrentViewers(0)),
                other.id().toString(), "",
                objectMapper.writeValueAsString(other.withCurrentViewers(0)));
        assertThat(event).isEqualTo(new LiveFeedEvent.Resynced(40L));
    }

    @Test
    void snapshot_runsReadScript_andReturnsCardsNewestFirst() throws Exception {
        var older = card(CATEGORY_ID).withCurrentViewers(0);
        var newer = new LiveFeedCard(UUID.randomUUID(), SELLER_ID, null, "Newer", null, null, 0L,
                Instant.parse("2026-10-06T12:00:00Z"), CATEGORY_ID);
        var call = new ScriptCall().replying("{\"version\":9,\"cards\":["
                + objectMapper.writeValueAsString(older) + "," + objectMapper.writeValueAsString(newer)
                + "],\"counts\":" + COUNTS_JSON + "}");

        var snapshot = adapter.snapshot();

        assertThat(call.script).isSameAs(RedisLiveFeedAdapter.SNAPSHOT_SCRIPT);
        assertThat(call.keys).isEqualTo(KEYS);
        assertThat(snapshot.version()).isEqualTo(9L);
        assertThat(snapshot.cards()).containsExactly(newer, older);
        assertThat(snapshot.counts()).containsExactly(new CategoryLiveCount(CATEGORY_ID, 3));
    }

    @Test
    void currentVersion_readsVersionKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("live:feed:version")).thenReturn("17");

        assertThat(adapter.currentVersion()).isEqualTo(17L);
    }

    @Test
    void currentVersion_missingKey_isZero() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("live:feed:version")).thenReturn(null);

        assertThat(adapter.currentVersion()).isZero();
    }

    @Test
    void tryAcquireReconcileLock_setsLockKeyIfAbsentWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("live:feed:reconcile-lock"), anyString(), eq(Duration.ofSeconds(54))))
                .thenReturn(true);

        assertThat(adapter.tryAcquireReconcileLock(Duration.ofSeconds(54))).isTrue();
    }

    @Test
    void tryAcquireReconcileLock_heldElsewhere_isFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("live:feed:reconcile-lock"), anyString(), any(Duration.class)))
                .thenReturn(false);

        assertThat(adapter.tryAcquireReconcileLock(Duration.ofSeconds(54))).isFalse();
    }

    @Test
    void writeScripts_areAtomicIncrAndPublish() {
        for (var script : List.of(RedisLiveFeedAdapter.UPSERT_SCRIPT, RedisLiveFeedAdapter.REMOVE_SCRIPT,
                RedisLiveFeedAdapter.REPLACE_ALL_SCRIPT)) {
            assertThat(script.getScriptAsString()).contains("'INCR'").contains("'PUBLISH'");
            assertThat(script.getResultType()).isEqualTo(String.class);
        }
        assertThat(RedisLiveFeedAdapter.SNAPSHOT_SCRIPT.getScriptAsString())
                .doesNotContain("'INCR'").doesNotContain("'PUBLISH'");
    }
}
