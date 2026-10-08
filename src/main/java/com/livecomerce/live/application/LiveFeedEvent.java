package com.livecomerce.live.application;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.livecomerce.live.application.port.out.LoadLivePort.CategoryLiveCount;

import java.util.List;
import java.util.UUID;

/**
 * A versioned change to the active-lives feed snapshot. {@code seq} is the snapshot
 * version right after the change was applied, so a client holding version {@code V}
 * applies only events with {@code seq > V}. Every event carries the full per-category
 * counts of the feed after the change, so clients never refetch the chips bar.
 *
 * <p>Wire shape (also the Redis Pub/Sub payload): a {@code type} discriminator plus the
 * record fields, e.g. {@code {"type":"live-added","seq":12,"card":{...},"counts":[...]}}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LiveFeedEvent.Added.class, name = LiveFeedEvent.Added.TYPE),
        @JsonSubTypes.Type(value = LiveFeedEvent.Removed.class, name = LiveFeedEvent.Removed.TYPE),
        @JsonSubTypes.Type(value = LiveFeedEvent.Resynced.class, name = LiveFeedEvent.Resynced.TYPE)
})
public sealed interface LiveFeedEvent {

    long seq();

    /** A live entered the feed or its card changed (clients replace by {@code card.id}). */
    record Added(long seq, LiveFeedCard card, List<CategoryLiveCount> counts) implements LiveFeedEvent {
        public static final String TYPE = "live-added";
    }

    /** A live left the feed. */
    record Removed(long seq, UUID liveId, List<CategoryLiveCount> counts) implements LiveFeedEvent {
        public static final String TYPE = "live-removed";
    }

    /** The whole snapshot was rebuilt from the database: clients must refetch it. */
    record Resynced(long seq) implements LiveFeedEvent {
        public static final String TYPE = "resync";
    }
}
