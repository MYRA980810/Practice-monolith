package com.livecomerce.live.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisViewerCountAdapterTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    RedisViewerCountAdapter adapter;

    private static final UUID LIVE_ID = UUID.randomUUID();
    private static final String KEY    = "live:" + LIVE_ID + ":viewers";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        adapter = new RedisViewerCountAdapter(redisTemplate);
    }

    @Test
    void increment_callsIncrOnCorrectKey_andReturnsNewCount() {
        when(valueOps.increment(KEY)).thenReturn(5L);

        long result = adapter.increment(LIVE_ID);

        assertThat(result).isEqualTo(5L);
        verify(valueOps).increment(KEY);
    }

    @Test
    void decrement_callsDecrOnCorrectKey_andReturnsNewCount() {
        when(valueOps.decrement(KEY)).thenReturn(3L);

        long result = adapter.decrement(LIVE_ID);

        assertThat(result).isEqualTo(3L);
        verify(valueOps).decrement(KEY);
    }

    @Test
    void decrement_whenResultNegative_clampsToZeroAndSetsKey() {
        when(valueOps.decrement(KEY)).thenReturn(-1L);

        long result = adapter.decrement(LIVE_ID);

        assertThat(result).isZero();
        verify(valueOps).set(KEY, "0");
    }

    @Test
    void get_returnsCurrentCount() {
        when(valueOps.get(KEY)).thenReturn("7");

        long result = adapter.get(LIVE_ID);

        assertThat(result).isEqualTo(7L);
    }

    @Test
    void get_whenKeyAbsent_returnsZero() {
        when(valueOps.get(KEY)).thenReturn(null);

        long result = adapter.get(LIVE_ID);

        assertThat(result).isZero();
    }
}
