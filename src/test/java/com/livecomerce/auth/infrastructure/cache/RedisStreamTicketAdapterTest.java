package com.livecomerce.auth.infrastructure.cache;

import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.livecomerce.live.StreamTicketStoreUnavailableException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisStreamTicketAdapterTest {

    private static final long TTL_MS = 30_000;

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    final RefreshTokenHasher hasher = new RefreshTokenHasher();
    RedisStreamTicketAdapter adapter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        adapter = new RedisStreamTicketAdapter(redisTemplate, hasher, TTL_MS);
    }

    @Test
    void issue_storesUserIdUnderTheTicketHashWithTtl_neverTheRawTicket() {
        var userId = UUID.randomUUID();

        var ticket = adapter.issue(userId);

        var key = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(key.capture(), eq(userId.toString()), eq(Duration.ofMillis(TTL_MS)));
        assertThat(key.getValue()).isEqualTo("auth:stream-ticket:" + hasher.hash(ticket.value()));
        assertThat(key.getValue()).doesNotContain(ticket.value());
        assertThat(ticket.ttl()).isEqualTo(Duration.ofMillis(TTL_MS));
    }

    @Test
    void issue_returns256BitBase64UrlTickets_thatAreUnique() {
        var first = adapter.issue(UUID.randomUUID()).value();
        var second = adapter.issue(UUID.randomUUID()).value();

        assertThat(Base64.getUrlDecoder().decode(first)).hasSize(32);
        assertThat(first).matches("[A-Za-z0-9_-]+");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void consume_redeemsWithAtomicGetAndDeleteOnTheHashedKey() {
        var userId = UUID.randomUUID();
        var key = "auth:stream-ticket:" + hasher.hash("the-ticket");
        when(valueOps.getAndDelete(key)).thenReturn(userId.toString());

        var result = adapter.consume("the-ticket");

        assertThat(result).contains(userId);
        verify(valueOps).getAndDelete(key);
        verifyNoMoreInteractions(valueOps);
    }

    @Test
    void consume_unknownExpiredOrUsedTicket_returnsEmpty() {
        when(valueOps.getAndDelete(anyString())).thenReturn(null);

        assertThat(adapter.consume("missing")).isEmpty();
    }

    @Test
    void consume_blankTicket_returnsEmptyWithoutTouchingRedis() {
        assertThat(adapter.consume(null)).isEmpty();
        assertThat(adapter.consume("  ")).isEmpty();

        verify(valueOps, never()).getAndDelete(anyString());
    }

    @Test
    void consume_corruptStoredValue_returnsEmpty() {
        when(valueOps.getAndDelete(anyString())).thenReturn("not-a-uuid");

        assertThat(adapter.consume("t")).isEmpty();
    }

    // --- Redis down: translated to the port's exception, never "invalid ticket" ---

    @Test
    void issue_redisFailure_isTranslatedToStoreUnavailable() {
        var cause = new RedisConnectionFailureException("connection refused");
        doThrow(cause).when(valueOps).set(anyString(), anyString(), any(Duration.class));

        assertThatThrownBy(() -> adapter.issue(UUID.randomUUID()))
                .isInstanceOf(StreamTicketStoreUnavailableException.class)
                .hasCause(cause);
    }

    @Test
    void consume_redisFailure_isTranslatedToStoreUnavailable_withoutLeakingTheTicket() {
        var cause = new QueryTimeoutException("timed out");
        when(valueOps.getAndDelete(anyString())).thenThrow(cause);

        assertThatThrownBy(() -> adapter.consume("secret-ticket"))
                .isInstanceOf(StreamTicketStoreUnavailableException.class)
                .hasCause(cause)
                .message().doesNotContain("secret-ticket");
    }
}
