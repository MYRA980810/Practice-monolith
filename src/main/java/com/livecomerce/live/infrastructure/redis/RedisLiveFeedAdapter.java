package com.livecomerce.live.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.LiveFeedCard;
import com.livecomerce.live.application.LiveFeedEvent;
import com.livecomerce.live.application.LiveFeedSnapshot;
import com.livecomerce.live.application.port.out.LiveFeedPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Active-lives feed snapshot in Redis, shared by every instance:
 * <ul>
 *   <li>{@code live:feed:cards} — hash liveId → card JSON ({@code currentViewers} stored as 0)</li>
 *   <li>{@code live:feed:card-category} — hash liveId → categoryId ({@code ""} when uncategorized),
 *       so a category move can decrement the old count without parsing card JSON in Lua</li>
 *   <li>{@code live:feed:counts} — hash categoryId → number of LIVE cards (zero counts deleted)</li>
 *   <li>{@code live:feed:version} — snapshot version counter</li>
 *   <li>{@code live:feed:reconcile-lock} — drift-reconciliation lease ({@code SET NX PX}), so one
 *       instance reconciles per cycle</li>
 * </ul>
 * Each write is one Lua script: apply the change, {@code INCR} the version, build the event
 * JSON with that version as {@code seq}, {@code PUBLISH} it to {@value #CHANNEL} and return
 * it. Scripts run atomically, so the snapshot, the version and the published event never
 * diverge and events are published in {@code seq} order. Event JSON is assembled in Lua
 * (rather than {@code cjson}) to keep integers integral; it matches the Jackson shape of
 * {@link LiveFeedEvent}.
 *
 * <p>The version never regresses, even after a Redis flush: {@code replaceAll}, and any write
 * that finds the version key missing, seed it to at least the caller's epoch millis
 * ({@code ARGV[2]}); every other write simply {@code INCR}s, keeping {@code seq} gap-free.
 */
@Component
@Profile("!local")
class RedisLiveFeedAdapter implements LiveFeedPort {

    static final String CHANNEL = "live:feed";

    private static final String CARDS_KEY         = "live:feed:cards";
    private static final String CARD_CATEGORY_KEY = "live:feed:card-category";
    private static final String COUNTS_KEY        = "live:feed:counts";
    private static final String VERSION_KEY       = "live:feed:version";
    private static final List<String> KEYS = List.of(CARDS_KEY, CARD_CATEGORY_KEY, COUNTS_KEY, VERSION_KEY);
    private static final String RECONCILE_LOCK_KEY = "live:feed:reconcile-lock";

    /** Shared helpers. KEYS: 1 cards, 2 card-category, 3 counts, 4 version. */
    private static final String LUA_HELPERS = """
            local function adjust(categoryId, delta)
              if not categoryId or categoryId == '' then return end
              if redis.call('HINCRBY', KEYS[3], categoryId, delta) <= 0 then
                redis.call('HDEL', KEYS[3], categoryId)
              end
            end
            local function counts_json()
              local flat = redis.call('HGETALL', KEYS[3])
              local items = {}
              for i = 1, #flat, 2 do
                items[#items + 1] = { flat[i], tonumber(flat[i + 1]) }
              end
              table.sort(items, function(a, b)
                if a[2] ~= b[2] then return a[2] > b[2] end
                return a[1] < b[1]
              end)
              local parts = {}
              for i, item in ipairs(items) do
                parts[i] = '{"categoryId":"' .. item[1] .. '","count":' .. string.format('%d', item[2]) .. '}'
              end
              return '[' .. table.concat(parts, ',') .. ']'
            end
            """;

    /**
     * Bumps the version; only write scripts include it. With {@code seed} (or when the version key
     * is missing, i.e. never built or flushed) the result is raised to at least {@code ARGV[2]},
     * the caller's epoch millis, so a rebuilt version never falls below one clients already saw.
     */
    private static final String LUA_NEXT_SEQ = """
            local function next_seq(seed)
              local fresh = redis.call('EXISTS', KEYS[4]) == 0
              local seq = redis.call('INCR', KEYS[4])
              local floor = tonumber(ARGV[2])
              if (seed or fresh) and seq < floor then
                seq = floor
                redis.call('SET', KEYS[4], ARGV[2])
              end
              return string.format('%d', seq)
            end
            """;

    /** ARGV: 1 channel, 2 now epoch millis, 3 liveId, 4 categoryId or "", 5 card JSON. */
    static final DefaultRedisScript<String> UPSERT_SCRIPT = new DefaultRedisScript<>(LUA_HELPERS + LUA_NEXT_SEQ + """
            local previous = redis.call('HGET', KEYS[2], ARGV[3])
            if previous ~= ARGV[4] then
              adjust(previous, -1)
              adjust(ARGV[4], 1)
            end
            redis.call('HSET', KEYS[1], ARGV[3], ARGV[5])
            redis.call('HSET', KEYS[2], ARGV[3], ARGV[4])
            local event = '{"type":"live-added","seq":' .. next_seq(false)
              .. ',"card":' .. ARGV[5] .. ',"counts":' .. counts_json() .. '}'
            redis.call('PUBLISH', ARGV[1], event)
            return event
            """, String.class);

    /** ARGV: 1 channel, 2 now epoch millis, 3 liveId. Returns nil — no version bump, nothing published — when absent. */
    static final DefaultRedisScript<String> REMOVE_SCRIPT = new DefaultRedisScript<>(LUA_HELPERS + LUA_NEXT_SEQ + """
            if redis.call('HEXISTS', KEYS[1], ARGV[3]) == 0 then return false end
            adjust(redis.call('HGET', KEYS[2], ARGV[3]), -1)
            redis.call('HDEL', KEYS[1], ARGV[3])
            redis.call('HDEL', KEYS[2], ARGV[3])
            local event = '{"type":"live-removed","seq":' .. next_seq(false)
              .. ',"liveId":"' .. ARGV[3] .. '","counts":' .. counts_json() .. '}'
            redis.call('PUBLISH', ARGV[1], event)
            return event
            """, String.class);

    /**
     * ARGV: 1 channel, 2 now epoch millis, then (liveId, categoryId or "", card JSON) per card.
     * The version becomes max(INCR, now) — monotonic across flushes, never reset.
     */
    static final DefaultRedisScript<String> REPLACE_ALL_SCRIPT = new DefaultRedisScript<>(LUA_HELPERS + LUA_NEXT_SEQ + """
            redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])
            for i = 3, #ARGV, 3 do
              redis.call('HSET', KEYS[1], ARGV[i], ARGV[i + 2])
              redis.call('HSET', KEYS[2], ARGV[i], ARGV[i + 1])
              adjust(ARGV[i + 1], 1)
            end
            local event = '{"type":"resync","seq":' .. next_seq(true) .. '}'
            redis.call('PUBLISH', ARGV[1], event)
            return event
            """, String.class);

    /** Read-only: version, cards and counts read in one atomic step so they're mutually consistent. */
    static final DefaultRedisScript<String> SNAPSHOT_SCRIPT = new DefaultRedisScript<>(LUA_HELPERS + """
            local version = redis.call('GET', KEYS[4]) or '0'
            local cards = redis.call('HVALS', KEYS[1])
            return '{"version":' .. version .. ',"cards":[' .. table.concat(cards, ',')
              .. '],"counts":' .. counts_json() .. '}'
            """, String.class);

    private static final Comparator<LiveFeedCard> NEWEST_FIRST =
            Comparator.comparing(LiveFeedCard::startedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;
    private final LongSupplier        nowMillis;
    /** Identifies this instance as the lease holder (diagnostics only; the lease just expires). */
    private final String              instanceId = UUID.randomUUID().toString();

    @Autowired
    RedisLiveFeedAdapter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this(redisTemplate, objectMapper, System::currentTimeMillis);
    }

    RedisLiveFeedAdapter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, LongSupplier nowMillis) {
        this.redisTemplate = redisTemplate;
        this.objectMapper  = objectMapper;
        this.nowMillis     = nowMillis;
    }

    @Override
    public LiveFeedEvent upsert(LiveFeedCard card) {
        var stored = card.withCurrentViewers(0);
        var json = redisTemplate.execute(UPSERT_SCRIPT, KEYS,
                CHANNEL, now(), stored.id().toString(), categoryArg(stored), toJson(stored));
        return parse(json, LiveFeedEvent.class);
    }

    @Override
    public Optional<LiveFeedEvent> remove(UUID liveId) {
        var json = redisTemplate.execute(REMOVE_SCRIPT, KEYS, CHANNEL, now(), liveId.toString());
        return json == null ? Optional.empty() : Optional.of(parse(json, LiveFeedEvent.class));
    }

    @Override
    public LiveFeedSnapshot snapshot() {
        var raw = parse(redisTemplate.execute(SNAPSHOT_SCRIPT, KEYS), LiveFeedSnapshot.class);
        var cards = raw.cards().stream().sorted(NEWEST_FIRST).toList();
        return new LiveFeedSnapshot(raw.version(), cards, raw.counts());
    }

    @Override
    public long currentVersion() {
        var value = redisTemplate.opsForValue().get(VERSION_KEY);
        if (value == null) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public LiveFeedEvent replaceAll(List<LiveFeedCard> cards) {
        var args = new ArrayList<Object>(2 + cards.size() * 3);
        args.add(CHANNEL);
        args.add(now());
        for (var card : cards) {
            var stored = card.withCurrentViewers(0);
            args.add(stored.id().toString());
            args.add(categoryArg(stored));
            args.add(toJson(stored));
        }
        return parse(redisTemplate.execute(REPLACE_ALL_SCRIPT, KEYS, args.toArray()), LiveFeedEvent.class);
    }

    @Override
    public boolean tryAcquireReconcileLock(Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(RECONCILE_LOCK_KEY, instanceId, ttl));
    }

    private String now() {
        return String.valueOf(nowMillis.getAsLong());
    }

    private static String categoryArg(LiveFeedCard card) {
        return card.categoryId() == null ? "" : card.categoryId().toString();
    }

    private String toJson(LiveFeedCard card) {
        try {
            return objectMapper.writeValueAsString(card);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize live feed card " + card.id(), e);
        }
    }

    private <T> T parse(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Malformed live feed payload from Redis: " + json, e);
        }
    }
}
