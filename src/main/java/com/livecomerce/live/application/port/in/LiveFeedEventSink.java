package com.livecomerce.live.application.port.in;

import com.livecomerce.live.application.LiveFeedEvent;

/**
 * Receives every {@link LiveFeedEvent} published to the feed, on every instance (e.g. the
 * SSE emitter registry that forwards it to the connected buyers). Implementations must not
 * block: delivery happens on the Pub/Sub listener thread (or the writer's thread in the
 * {@code local} profile). A failing sink is logged and skipped, never retried.
 */
public interface LiveFeedEventSink {

    void deliver(LiveFeedEvent event);
}
