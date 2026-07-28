package com.livecomerce.live.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryViewerCountAdapterTest {

    InMemoryViewerCountAdapter adapter;

    private static final UUID LIVE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adapter = new InMemoryViewerCountAdapter();
    }

    @Test
    void increment_returnsIncreasingCount() {
        assertThat(adapter.increment(LIVE_ID)).isEqualTo(1L);
        assertThat(adapter.increment(LIVE_ID)).isEqualTo(2L);
    }

    @Test
    void decrement_clampsAtZero() {
        long result = adapter.decrement(LIVE_ID);

        assertThat(result).isZero();
    }

    @Test
    void get_whenNoActivity_returnsZero() {
        assertThat(adapter.get(LIVE_ID)).isZero();
    }

    @Test
    void heartbeat_singleViewer_returnsCountOfOne() {
        long result = adapter.heartbeat(LIVE_ID, "viewer-1");

        assertThat(result).isEqualTo(1L);
    }

    @Test
    void heartbeat_multipleDistinctViewers_returnsAccumulatedCount() {
        adapter.heartbeat(LIVE_ID, "viewer-1");
        adapter.heartbeat(LIVE_ID, "viewer-2");
        long result = adapter.heartbeat(LIVE_ID, "viewer-3");

        assertThat(result).isEqualTo(3L);
    }

    @Test
    void heartbeat_sameViewerRepeated_doesNotDoubleCount() {
        adapter.heartbeat(LIVE_ID, "viewer-1");
        long result = adapter.heartbeat(LIVE_ID, "viewer-1");

        assertThat(result).isEqualTo(1L);
    }

    @Test
    void heartbeat_differentLives_areIsolated() {
        var otherLiveId = UUID.randomUUID();

        adapter.heartbeat(LIVE_ID, "viewer-1");
        long result = adapter.heartbeat(otherLiveId, "viewer-1");

        assertThat(result).isEqualTo(1L);
    }
}
