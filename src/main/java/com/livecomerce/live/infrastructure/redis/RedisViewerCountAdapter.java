package com.livecomerce.live.infrastructure.redis;

import com.livecomerce.live.application.port.out.ViewerCountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("!local")
@RequiredArgsConstructor
class RedisViewerCountAdapter implements ViewerCountPort {

    private final StringRedisTemplate redisTemplate;

    @Override
    public long increment(UUID liveId) {
        Long result = redisTemplate.opsForValue().increment(key(liveId));
        return result == null ? 0L : result;
    }

    @Override
    public long decrement(UUID liveId) {
        Long result = redisTemplate.opsForValue().decrement(key(liveId));
        if (result == null || result < 0) {
            redisTemplate.opsForValue().set(key(liveId), "0");
            return 0L;
        }
        return result;
    }

    @Override
    public long get(UUID liveId) {
        String value = redisTemplate.opsForValue().get(key(liveId));
        if (value == null) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String key(UUID liveId) {
        return "live:" + liveId + ":viewers";
    }
}
