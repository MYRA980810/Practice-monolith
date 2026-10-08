package com.livecomerce.live.application;

import com.livecomerce.live.LiveCancelledEvent;
import com.livecomerce.live.LiveCategoryChangedEvent;
import com.livecomerce.live.LiveEndedEvent;
import com.livecomerce.live.LiveReconnectingEvent;
import com.livecomerce.live.LiveRevivedEvent;
import com.livecomerce.live.LiveStartedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LiveFeedEventListenerTest {

    @Mock LiveFeedSyncService liveFeedSyncService;
    @InjectMocks LiveFeedEventListener sut;

    private static final UUID LIVE_ID   = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final Instant NOW    = Instant.now();

    @Test
    void onStarted_syncsLive() {
        sut.on(new LiveStartedEvent(LIVE_ID, null, "t", List.of(), NOW));
        verify(liveFeedSyncService).sync(LIVE_ID);
    }

    @Test
    void onEnded_syncsLive() {
        sut.on(new LiveEndedEvent(LIVE_ID, SELLER_ID, null, NOW));
        verify(liveFeedSyncService).sync(LIVE_ID);
    }

    @Test
    void onReconnecting_syncsLive() {
        sut.on(new LiveReconnectingEvent(LIVE_ID, SELLER_ID, "stream-disconnected"));
        verify(liveFeedSyncService).sync(LIVE_ID);
    }

    @Test
    void onRevived_syncsLive() {
        sut.on(new LiveRevivedEvent(LIVE_ID, SELLER_ID, null, NOW));
        verify(liveFeedSyncService).sync(LIVE_ID);
    }

    @Test
    void onCategoryChanged_syncsLive() {
        sut.on(new LiveCategoryChangedEvent(LIVE_ID, UUID.randomUUID()));
        verify(liveFeedSyncService).sync(LIVE_ID);
    }

    @Test
    void onCancelled_whileLive_syncsLive() {
        sut.on(new LiveCancelledEvent(LIVE_ID, SELLER_ID, null, "t", List.of(), true, NOW));
        verify(liveFeedSyncService).sync(LIVE_ID);
    }

    @Test
    void onCancelled_whileScheduled_isIgnored() {
        sut.on(new LiveCancelledEvent(LIVE_ID, SELLER_ID, null, "t", List.of(), false, NOW));
        verifyNoInteractions(liveFeedSyncService);
    }

    @Test
    void syncFailure_propagates_soThePublicationStaysIncomplete() {
        doThrow(new IllegalStateException("redis down")).when(liveFeedSyncService).sync(LIVE_ID);

        assertThatThrownBy(() -> sut.on(new LiveEndedEvent(LIVE_ID, SELLER_ID, null, NOW)))
                .isInstanceOf(IllegalStateException.class);
    }
}
