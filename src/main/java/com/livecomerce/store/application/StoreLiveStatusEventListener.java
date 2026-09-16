package com.livecomerce.store.application;

import com.livecomerce.live.LiveEndedEvent;
import com.livecomerce.live.LiveStartedEvent;
import com.livecomerce.store.application.port.out.SaveStoreLiveStatusPort;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Projects live start/end into the local {@code store_live_status} row so
 * store-card and listing reads never cross into {@code live} at request time
 * (see {@link com.livecomerce.store.application.port.out.LoadStoreLiveStatusPort}).
 * Fired on every close path, including the stale-live auto-close job — {@code
 * EndLiveService.close} publishes {@link LiveEndedEvent} unconditionally, whether
 * the live ended via the explicit end endpoint or {@code endStaleLive}.
 *
 * <p>Both events carry their own domain {@code occurredAt} timestamp (the live's
 * {@code startedAt}/{@code endedAt}, not delivery time), forwarded to {@link
 * SaveStoreLiveStatusPort} so an out-of-order redelivery of either event is
 * rejected instead of resurrecting a stale status.
 */
@Component
@RequiredArgsConstructor
class StoreLiveStatusEventListener {

    private final SaveStoreLiveStatusPort saveStoreLiveStatusPort;

    @ApplicationModuleListener
    void on(LiveStartedEvent event) {
        if (event.storeId() == null) {
            // SELLER_PROFILE-context lives have no store to project onto.
            return;
        }
        saveStoreLiveStatusPort.markLive(event.storeId(), event.liveId(), event.occurredAt());
    }

    @ApplicationModuleListener
    void on(LiveEndedEvent event) {
        if (event.storeId() == null) {
            // SELLER_PROFILE-context lives have no store to project onto.
            return;
        }
        saveStoreLiveStatusPort.markEnded(event.storeId(), event.liveId(), event.occurredAt());
    }
}
