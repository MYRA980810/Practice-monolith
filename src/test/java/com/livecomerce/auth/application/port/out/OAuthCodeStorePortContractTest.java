package com.livecomerce.auth.application.port.out;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthCodeStorePortContractTest {

    /**
     * Compile-only contract test: verifies both method signatures exist.
     */
    @Test
    void interface_implementableWithBothMethods() {
        OAuthCodeStorePort impl = new OAuthCodeStorePort() {
            @Override
            public void store(String code, OAuthCodePayload payload, Duration ttl) {
                // no-op
            }

            @Override
            public Optional<OAuthCodePayload> exchange(String code) {
                return Optional.empty();
            }
        };

        // store method compiles and is callable
        impl.store("code", new OAuthCodePayload(java.util.UUID.randomUUID(), OAuthTokenType.FULL), Duration.ofSeconds(90));

        // exchange method compiles, returns Optional
        Optional<OAuthCodePayload> result = impl.exchange("code");
        assertThat(result).isEmpty();
    }
}
