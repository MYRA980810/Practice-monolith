package com.livecomerce.live.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.LiveFeedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.SubscriptionListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Receives every feed event published on {@value RedisLiveFeedAdapter#CHANNEL} — by any
 * instance, including this one — and hands it to the local sinks. A malformed payload is
 * logged and dropped: throwing would only reach the listener container.
 *
 * <p>Pub/Sub has no replay: events published while the subscription was down are lost. The
 * container notifies every (re)subscription; on any after the first, a local {@link
 * LiveFeedEvent.Resynced} is dispatched so connected clients refetch the snapshot. Its {@code
 * seq} is the last one seen here — no Redis call, since the callback may run on the client's
 * I/O thread.
 */
@Component
@Profile("!local")
class RedisLiveFeedSubscriber implements MessageListener, SubscriptionListener {

    private static final Logger log = LoggerFactory.getLogger(RedisLiveFeedSubscriber.class);

    private final ObjectMapper            objectMapper;
    private final LiveFeedEventDispatcher dispatcher;
    private final AtomicBoolean           subscribedBefore = new AtomicBoolean();
    private final AtomicLong              lastSeq          = new AtomicLong();

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
        lastSeq.accumulateAndGet(event.seq(), Math::max);
        dispatcher.deliver(event);
    }

    @Override
    public void onChannelSubscribed(byte[] channel, long count) {
        if (!subscribedBefore.compareAndSet(false, true)) {
            log.warn("Live feed channel re-subscribed; dispatching resync (events may have been lost)");
            dispatcher.deliver(new LiveFeedEvent.Resynced(lastSeq.get()));
        }
    }
}
