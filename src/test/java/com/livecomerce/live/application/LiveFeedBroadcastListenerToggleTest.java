package com.livecomerce.live.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** The Agora "lives-feed" broadcast stays on by default until the frontend moves to SSE. */
class LiveFeedBroadcastListenerToggleTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(AgoraRtmMessagePort.class, () -> mock(AgoraRtmMessagePort.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withUserConfiguration(LiveFeedBroadcastListener.class);

    @Test
    void enabledWhenPropertyMissing() {
        runner.run(context -> assertThat(context).hasSingleBean(LiveFeedBroadcastListener.class));
    }

    @Test
    void enabledWhenPropertyTrue() {
        runner.withPropertyValues("live.feed.agora.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(LiveFeedBroadcastListener.class));
    }

    @Test
    void disabledWhenPropertyFalse() {
        runner.withPropertyValues("live.feed.agora.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(LiveFeedBroadcastListener.class));
    }
}
