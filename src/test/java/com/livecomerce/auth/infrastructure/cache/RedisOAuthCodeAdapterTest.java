package com.livecomerce.auth.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.auth.application.port.out.OAuthCodePayload;
import com.livecomerce.auth.application.port.out.OAuthTokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisOAuthCodeAdapterTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    // Real ObjectMapper for JSON serialization
    ObjectMapper objectMapper = new ObjectMapper();

    RedisOAuthCodeAdapter adapter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        adapter = new RedisOAuthCodeAdapter(redisTemplate, objectMapper);
    }

    @Test
    void store_callsSetWithPrefixedKeyAndTtl() throws Exception {
        UUID userId = UUID.randomUUID();
        var payload = new OAuthCodePayload(userId, OAuthTokenType.FULL);
        Duration ttl = Duration.ofSeconds(90);
        String expectedJson = objectMapper.writeValueAsString(payload);

        adapter.store("my-code", payload, ttl);

        verify(valueOps).set(eq("oauth2:code:my-code"), eq(expectedJson), eq(ttl));
    }

    @Test
    void exchange_withNonNullRedisValue_deserializesAndReturnsPayload() throws Exception {
        UUID userId = UUID.randomUUID();
        var payload = new OAuthCodePayload(userId, OAuthTokenType.OAUTH_PENDING);
        String json = objectMapper.writeValueAsString(payload);
        when(valueOps.getAndDelete("oauth2:code:some-code")).thenReturn(json);

        var result = adapter.exchange("some-code");

        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo(userId);
        assertThat(result.get().tokenType()).isEqualTo(OAuthTokenType.OAUTH_PENDING);
    }

    @Test
    void exchange_withNullRedisValue_returnsEmptyOptional() {
        when(valueOps.getAndDelete("oauth2:code:missing")).thenReturn(null);

        var result = adapter.exchange("missing");

        assertThat(result).isEmpty();
    }

    @Test
    void exchange_callsGetAndDeleteExactlyOnce() {
        when(valueOps.getAndDelete("oauth2:code:once")).thenReturn(null);

        adapter.exchange("once");

        verify(valueOps).getAndDelete("oauth2:code:once");
        // verifyNoMoreInteractions on valueOps would also confirm no separate get+delete
    }
}
