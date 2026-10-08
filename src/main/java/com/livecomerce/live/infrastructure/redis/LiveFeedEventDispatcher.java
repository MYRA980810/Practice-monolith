package com.livecomerce.live.infrastructure.redis;

import com.livecomerce.live.application.LiveFeedEvent;
import com.livecomerce.live.application.port.in.LiveFeedEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Hands a feed event to every local {@link LiveFeedEventSink}. Sinks are resolved lazily
 * through {@link ObjectProvider} on each delivery: zero sinks is valid, and a sink that
 * itself depends on {@link com.livecomerce.live.application.port.out.LiveFeedPort} (to read
 * the snapshot) doesn't form a constructor cycle with the in-memory adapter. One failing
 * sink is logged and never stops the others or the caller (the Pub/Sub listener thread).
 */
@Component
class LiveFeedEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(LiveFeedEventDispatcher.class);

    private final ObjectProvider<LiveFeedEventSink> sinks;

    LiveFeedEventDispatcher(ObjectProvider<LiveFeedEventSink> sinks) {
        this.sinks = sinks;
    }

    void deliver(LiveFeedEvent event) {
        sinks.orderedStream().forEach(sink -> {
            try {
                sink.deliver(event);
            } catch (Exception e) {
                log.warn("Live feed sink {} failed on event seq {}: {}",
                        sink.getClass().getSimpleName(), event.seq(), e.getMessage());
            }
        });
    }
}
