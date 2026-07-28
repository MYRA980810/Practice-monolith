package com.livecomerce.live.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisViewerCountAdapterTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock ZSetOperations<String, String> zSetOps;

    RedisViewerCountAdapter adapter;

    private static final UUID LIVE_ID = UUID.randomUUID();
    private static final String KEY    = "live:" + LIVE_ID + ":viewers";
    private static final String HEARTBEAT_KEY = "live:" + LIVE_ID + ":viewers:heartbeat";

    @BeforeEach
    void setUp() {
        adapter = new RedisViewerCountAdapter(redisTemplate);
    }

    @Test
    void increment_callsIncrOnCorrectKey_andReturnsNewCount() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(KEY)).thenReturn(5L);

        long result = adapter.increment(LIVE_ID);

        assertThat(result).isEqualTo(5L);
        verify(valueOps).increment(KEY);
    }

    @Test
    void decrement_callsDecrOnCorrectKey_andReturnsNewCount() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.decrement(KEY)).thenReturn(3L);

        long result = adapter.decrement(LIVE_ID);

        assertThat(result).isEqualTo(3L);
        verify(valueOps).decrement(KEY);
    }

    @Test
    void decrement_whenResultNegative_clampsToZeroAndSetsKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.decrement(KEY)).thenReturn(-1L);

        long result = adapter.decrement(LIVE_ID);

        assertThat(result).isZero();
        verify(valueOps).set(KEY, "0");
    }

    @Test
    void get_returnsCurrentCount() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(KEY)).thenReturn("7");

        long result = adapter.get(LIVE_ID);

        assertThat(result).isEqualTo(7L);
    }

    @Test
    void get_whenKeyAbsent_returnsZero() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(KEY)).thenReturn(null);

        long result = adapter.get(LIVE_ID);

        assertThat(result).isZero();
    }

    // --- heartbeat ---

    @Test
    void heartbeat_addsViewerToZSet_prunesStaleEntries_andReturnsCount() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard(HEARTBEAT_KEY)).thenReturn(3L);

        long result = adapter.heartbeat(LIVE_ID, "viewer-1");

        assertThat(result).isEqualTo(3L);
        verify(zSetOps).add(eq(HEARTBEAT_KEY), eq("viewer-1"), anyDouble());
        verify(zSetOps).removeRangeByScore(eq(HEARTBEAT_KEY), eq(0.0), anyDouble());
        verify(zSetOps).zCard(HEARTBEAT_KEY);
    }

    @Test
    void heartbeat_whenZCardReturnsNull_returnsZero() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard(HEARTBEAT_KEY)).thenReturn(null);

        long result = adapter.heartbeat(LIVE_ID, "viewer-1");

        assertThat(result).isZero();
    }

    @Test
    void heartbeat_setsTtlOnHeartbeatKey() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard(HEARTBEAT_KEY)).thenReturn(1L);

        adapter.heartbeat(LIVE_ID, "viewer-1");

        verify(redisTemplate).expire(eq(HEARTBEAT_KEY), eq(Duration.ofSeconds(120)));
    }
}
