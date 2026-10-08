package com.livecomerce.live.application;

import com.livecomerce.live.LiveCancelledEvent;
import com.livecomerce.live.LiveCategoryChangedEvent;
import com.livecomerce.live.LiveEndedEvent;
import com.livecomerce.live.LiveReconnectingEvent;
import com.livecomerce.live.LiveRevivedEvent;
import com.livecomerce.live.LiveStartedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Feeds every live transition that can change the active feed into {@link
 * LiveFeedSyncService#sync}, which re-reads the live and upserts or removes its card. The
 * event only says <em>which</em> live to look at, never what to do with it — see {@link
 * LiveFeedSyncService} for why that makes delivery order irrelevant.
 *
 * <p>Failures propagate (like {@code StoreLiveStatusEventListener}): the Modulith event
 * publication stays incomplete and can be resubmitted, and the reconciliation job repairs
 * the snapshot within a cycle regardless.
 */
@Component
@RequiredArgsConstructor
class LiveFeedEventListener {

    private final LiveFeedSyncService liveFeedSyncService;

    @ApplicationModuleListener
    void on(LiveStartedEvent event) {
        liveFeedSyncService.sync(event.liveId());
    }

    @ApplicationModuleListener
    void on(LiveEndedEvent event) {
        liveFeedSyncService.sync(event.liveId());
    }

    @ApplicationModuleListener
    void on(LiveReconnectingEvent event) {
        liveFeedSyncService.sync(event.liveId());
    }

    @ApplicationModuleListener
    void on(LiveRevivedEvent event) {
        liveFeedSyncService.sync(event.liveId());
    }

    @ApplicationModuleListener
    void on(LiveCategoryChangedEvent event) {
        liveFeedSyncService.sync(event.liveId());
    }

    @ApplicationModuleListener
    void on(LiveCancelledEvent event) {
        if (!event.wasLive()) {
            // Cancelled while SCHEDULED: it was never on the active feed.
            return;
        }
        liveFeedSyncService.sync(event.liveId());
    }
}
