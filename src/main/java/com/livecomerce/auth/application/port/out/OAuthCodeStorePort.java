package com.livecomerce.auth.application.port.out;

import java.time.Duration;
import java.util.Optional;

public interface OAuthCodeStorePort {
    void store(String code, OAuthCodePayload payload, Duration ttl);
    Optional<OAuthCodePayload> exchange(String code);
}
