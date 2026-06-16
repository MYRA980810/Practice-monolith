package com.livecomerce.auth.infrastructure.cache;

import com.livecomerce.auth.application.port.out.OAuthCodePayload;
import com.livecomerce.auth.application.port.out.OAuthCodeStorePort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("local")
class InMemoryOAuthCodeAdapter implements OAuthCodeStorePort {

    private final ConcurrentHashMap<String, OAuthCodePayload> store = new ConcurrentHashMap<>();

    @Override
    public void store(String code, OAuthCodePayload payload, Duration ttl) {
        store.put(code, payload);
    }

    @Override
    public Optional<OAuthCodePayload> exchange(String code) {
        return Optional.ofNullable(store.remove(code));
    }
}
