package com.livecomerce.live.infrastructure.ivs;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class IvsChatClientConfigTest {

    @Test
    void ivschatClient_appliesConfiguredApiCallTimeouts() {
        var config = new IvsChatClientConfig();
        ReflectionTestUtils.setField(config, "ivsRegion", "us-east-1");
        ReflectionTestUtils.setField(config, "apiCallTimeoutMs", 3000L);
        ReflectionTestUtils.setField(config, "apiCallAttemptTimeoutMs", 1500L);

        try (var client = config.ivschatClient()) {
            var overrides = client.serviceClientConfiguration().overrideConfiguration();

            assertThat(overrides.apiCallTimeout()).contains(Duration.ofMillis(3000));
            assertThat(overrides.apiCallAttemptTimeout()).contains(Duration.ofMillis(1500));
        }
    }
}
