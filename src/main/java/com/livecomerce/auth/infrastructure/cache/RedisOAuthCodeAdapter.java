package com.livecomerce.auth.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.auth.application.port.out.OAuthCodePayload;
import com.livecomerce.auth.application.port.out.OAuthCodeStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@Profile("!local")
@RequiredArgsConstructor
public class RedisOAuthCodeAdapter implements OAuthCodeStorePort {

    private static final String KEY_PREFIX = "oauth2:code:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public void store(String code, OAuthCodePayload payload, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            redis.opsForValue().set(KEY_PREFIX + code, json, ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize OAuthCodePayload", e);
        }
    }

    @Override
    public Optional<OAuthCodePayload> exchange(String code) {
        String json = redis.opsForValue().getAndDelete(KEY_PREFIX + code);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, OAuthCodePayload.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize OAuthCodePayload", e);
        }
    }
}
