package com.livecomerce.auth.infrastructure.cache;

import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import com.livecomerce.live.StreamTicketPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Local-profile twin of {@link RedisStreamTicketAdapter}: same hashing, single use and expiry. */
@Component
@Profile("local")
class InMemoryStreamTicketAdapter implements StreamTicketPort {

    private record Entry(UUID userId, Instant expiresAt) {}

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
    private final RefreshTokenHasher hasher;
    private final Clock clock;
    private final Duration ttl;

    InMemoryStreamTicketAdapter(RefreshTokenHasher hasher,
                                Clock clock,
                                @Value("${auth.stream-ticket.ttl-ms:30000}") long ttlMs) {
        this.hasher = hasher;
        this.clock = clock;
        this.ttl = Duration.ofMillis(ttlMs);
    }

    @Override
    public StreamTicket issue(UUID userId) {
        var now = clock.instant();
        store.values().removeIf(e -> !now.isBefore(e.expiresAt()));
        var ticket = hasher.generateRawToken();
        store.put(hasher.hash(ticket), new Entry(userId, now.plus(ttl)));
        return new StreamTicket(ticket, ttl);
    }

    @Override
    public Optional<UUID> consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return Optional.empty();
        }
        var entry = store.remove(hasher.hash(ticket));
        if (entry == null || !clock.instant().isBefore(entry.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(entry.userId());
    }
}
