package com.livecomerce.live.infrastructure.redis;

import com.livecomerce.live.application.port.out.ViewerCountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@Profile("!local")
@RequiredArgsConstructor
class RedisViewerCountAdapter implements ViewerCountPort {

    private static final long HEARTBEAT_TTL_MILLIS = 30_000L;

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

    @Override
    public long heartbeat(UUID liveId, String viewerId) {
        String key = heartbeatKey(liveId);
        long now = System.currentTimeMillis();

        redisTemplate.opsForZSet().add(key, viewerId, now);
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, now - HEARTBEAT_TTL_MILLIS);
        redisTemplate.expire(key, Duration.ofSeconds(120));

        Long count = redisTemplate.opsForZSet().zCard(key);
        return count == null ? 0L : count;
    }

    private static String key(UUID liveId) {
        return "live:" + liveId + ":viewers";
    }

    private static String heartbeatKey(UUID liveId) {
        return "live:" + liveId + ":viewers:heartbeat";
    }

    @Override
    public boolean shouldBroadcast(UUID liveId, long count) {
        String key = lastBroadcastKey(liveId);
        String previous = redisTemplate.opsForValue().getAndSet(key, String.valueOf(count));
        redisTemplate.expire(key, Duration.ofSeconds(120));
        return previous == null || !previous.equals(String.valueOf(count));
    }

    private static String lastBroadcastKey(UUID liveId) {
        return "live:" + liveId + ":viewers:last-broadcast";
    }
}
