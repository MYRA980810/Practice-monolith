package com.livecomerce.live.infrastructure.redis;

import com.livecomerce.live.application.port.out.ViewerCountPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Profile("local")
class InMemoryViewerCountAdapter implements ViewerCountPort {

    private final ConcurrentHashMap<UUID, AtomicLong> store = new ConcurrentHashMap<>();

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
}
