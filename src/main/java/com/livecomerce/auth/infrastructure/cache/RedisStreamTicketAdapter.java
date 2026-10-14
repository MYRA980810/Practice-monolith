package com.livecomerce.auth.infrastructure.cache;

import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import com.livecomerce.live.StreamTicketPort;
import com.livecomerce.live.StreamTicketStoreUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
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
 * <p>
 * A Redis failure ({@link DataAccessException}: connection refused, timeout...) is translated to
 * {@link StreamTicketStoreUnavailableException}, so callers can tell an outage ("retry later") from
 * a bad ticket ("fetch a new one") without depending on Spring Data types. The message never
 * contains the ticket.
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
        try {
            redis.opsForValue().set(KEY_PREFIX + hasher.hash(ticket), userId.toString(), ttl);
        } catch (DataAccessException e) {
            throw new StreamTicketStoreUnavailableException("Stream ticket store unavailable on issue", e);
        }
        return new StreamTicket(ticket, ttl);
    }

    @Override
    public Optional<UUID> consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return Optional.empty();
        }
        String stored;
        try {
            stored = redis.opsForValue().getAndDelete(KEY_PREFIX + hasher.hash(ticket));
        } catch (DataAccessException e) {
            throw new StreamTicketStoreUnavailableException("Stream ticket store unavailable on consume", e);
        }
        return StreamTickets.parseUserId(stored);
    }
}
