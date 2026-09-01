package com.livecomerce.live.infrastructure.redis;

import com.livecomerce.live.application.port.out.ViewerCountPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Profile("local")
class InMemoryViewerCountAdapter implements ViewerCountPort {

    private static final long HEARTBEAT_TTL_SECONDS = 30L;

    private final ConcurrentHashMap<UUID, AtomicLong> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Instant>> heartbeats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicLong> lastBroadcast = new ConcurrentHashMap<>();

    @Override
    public long increment(UUID liveId) {
        return store.computeIfAbsent(liveId, k -> new AtomicLong(0)).incrementAndGet();
    }

    @Override
    public long decrement(UUID liveId) {
        long result = store.computeIfAbsent(liveId, k -> new AtomicLong(0)).decrementAndGet();
        if (result < 0) {
            store.get(liveId).set(0);
            return 0L;
        }
        return result;
    }

    @Override
    public long get(UUID liveId) {
        AtomicLong counter = store.get(liveId);
        return counter == null ? 0L : counter.get();
    }

    @Override
    public long heartbeat(UUID liveId, String viewerId) {
        var viewers = heartbeats.computeIfAbsent(liveId, k -> new ConcurrentHashMap<>());
        viewers.put(viewerId, Instant.now());

        Instant cutoff = Instant.now().minusSeconds(HEARTBEAT_TTL_SECONDS);
        viewers.values().removeIf(seenAt -> seenAt.isBefore(cutoff));

        return viewers.size();
    }

    @Override
    public boolean shouldBroadcast(UUID liveId, long count) {
        var previous = lastBroadcast.computeIfAbsent(liveId, k -> new AtomicLong(-1));
        return previous.getAndSet(count) != count;
    }
}
