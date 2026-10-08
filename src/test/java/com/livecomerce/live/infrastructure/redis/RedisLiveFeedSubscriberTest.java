package com.livecomerce.live.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.livecomerce.live.application.LiveFeedEvent;
import com.livecomerce.live.application.port.out.LoadLivePort.CategoryLiveCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisLiveFeedSubscriberTest {

    @Mock LiveFeedEventDispatcher dispatcher;

    /** Configured like Spring Boot's auto-configured mapper (ISO-8601 dates), which the app injects. */
    final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();

    RedisLiveFeedSubscriber subscriber;

    @BeforeEach
    void setUp() {
        subscriber = new RedisLiveFeedSubscriber(objectMapper, dispatcher);
    }

    private static DefaultMessage message(String body) {
        return new DefaultMessage("live:feed".getBytes(StandardCharsets.UTF_8), body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void onMessage_removedEvent_isDeserializedAndDispatched() {
        var liveId     = UUID.randomUUID();
        var categoryId = UUID.randomUUID();

        subscriber.onMessage(message("{\"type\":\"live-removed\",\"seq\":5,\"liveId\":\"" + liveId
                + "\",\"counts\":[{\"categoryId\":\"" + categoryId + "\",\"count\":2}]}"), null);

        verify(dispatcher).deliver(new LiveFeedEvent.Removed(5L, liveId, List.of(new CategoryLiveCount(categoryId, 2))));
    }

    @Test
    void onMessage_resyncEvent_isDispatched() {
        subscriber.onMessage(message("{\"type\":\"resync\",\"seq\":30}"), null);

        verify(dispatcher).deliver(new LiveFeedEvent.Resynced(30L));
    }

    @Test
    void onMessage_malformedPayload_isDroppedWithoutThrowing() {
        assertThatCode(() -> subscriber.onMessage(message("not-json"), null)).doesNotThrowAnyException();

        verify(dispatcher, never()).deliver(any());
    }
}
