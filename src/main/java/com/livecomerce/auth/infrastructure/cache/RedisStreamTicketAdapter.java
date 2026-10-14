package com.livecomerce.auth.infrastructure.cache;

import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import com.livecomerce.live.StreamTicketPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Stream tickets in Redis: only the SHA-256 of the ticket is stored (a Redis dump never yields a
 * usable ticket), bound to the user id, with a short TTL. Redemption is a single GETDEL, so two
 * concurrent requests with the same ticket can't both succeed.
 */
@Component
@Profile("!local")
class RedisStreamTicketAdapter implements StreamTicketPort {

    static final String KEY_PREFIX = "auth:stream-ticket:";

    private final StringRedisTemplate redis;
    private final RefreshTokenHasher hasher;
    private final Duration ttl;

    RedisStreamTicketAdapter(StringRedisTemplate redis,
                             RefreshTokenHasher hasher,
                             @Value("${auth.stream-ticket.ttl-ms:30000}") long ttlMs) {
        this.redis = redis;
        this.hasher = hasher;
        this.ttl = Duration.ofMillis(ttlMs);
    }

    @Override
    public StreamTicket issue(UUID userId) {
        var ticket = hasher.generateRawToken();
        redis.opsForValue().set(KEY_PREFIX + hasher.hash(ticket), userId.toString(), ttl);
        return new StreamTicket(ticket, ttl);
    }

    @Override
    public Optional<UUID> consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return Optional.empty();
        }
        return StreamTickets.parseUserId(redis.opsForValue().getAndDelete(KEY_PREFIX + hasher.hash(ticket)));
    }
}
