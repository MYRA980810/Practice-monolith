package com.livecomerce.live.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.LiveFeedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Receives every feed event published on {@value RedisLiveFeedAdapter#CHANNEL} — by any
 * instance, including this one — and hands it to the local sinks. A malformed payload is
 * logged and dropped: throwing would only reach the listener container.
 */
@Component
@Profile("!local")
class RedisLiveFeedSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisLiveFeedSubscriber.class);

    private final ObjectMapper            objectMapper;
    private final LiveFeedEventDispatcher dispatcher;

    RedisLiveFeedSubscriber(ObjectMapper objectMapper, LiveFeedEventDispatcher dispatcher) {
        this.objectMapper = objectMapper;
        this.dispatcher   = dispatcher;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        var body = new String(message.getBody(), StandardCharsets.UTF_8);
        LiveFeedEvent event;
        try {
            event = objectMapper.readValue(body, LiveFeedEvent.class);
        } catch (Exception e) {
            log.warn("Dropping malformed live feed message: {}", e.getMessage());
            return;
        }
        dispatcher.deliver(event);
    }
}
