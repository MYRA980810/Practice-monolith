package com.livecomerce.store.application;

import com.livecomerce.live.LiveCancelledEvent;
import com.livecomerce.live.LiveEndedEvent;
import com.livecomerce.live.LiveStartedEvent;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.SaveStoreLiveStatusPort;
import com.livecomerce.store.domain.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Projects live start/end into the local {@code store_live_status} row so
 * store-card and listing reads never cross into {@code live} at request time
 * (see {@link com.livecomerce.store.application.port.out.LoadStoreLiveStatusPort}).
 * Fired on every close path, including the stale-live auto-close job — {@code
 * EndLiveService.close} publishes {@link LiveEndedEvent} unconditionally, whether
 * the live ended via the explicit end endpoint or {@code endStaleLive}. A live
 * cancelled while LIVE/RECONNECTING ({@link LiveCancelledEvent#wasLive()}) is
 * cleared the same way.
 *
 * <p>Lives belong to the seller; the store is the seller's optional catalog
 * channel ({@code stores.user_id} is unique). Each event's {@code sellerId} is
 * resolved to the seller's store here, and the event is ignored when the seller
 * has no store.
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
    private final LoadStorePort loadStorePort;

    @ApplicationModuleListener
    void on(LiveStartedEvent event) {
        storeIdOf(event.sellerId()).ifPresent(storeId ->
                saveStoreLiveStatusPort.markLive(storeId, event.liveId(), event.occurredAt()));
    }

    @ApplicationModuleListener
    void on(LiveEndedEvent event) {
        storeIdOf(event.sellerId()).ifPresent(storeId ->
                saveStoreLiveStatusPort.markEnded(storeId, event.liveId(), event.occurredAt()));
    }

    @ApplicationModuleListener
    void on(LiveCancelledEvent event) {
        if (!event.wasLive()) {
            // Cancelled while SCHEDULED: the store was never marked live.
            return;
        }
        storeIdOf(event.sellerId()).ifPresent(storeId ->
                saveStoreLiveStatusPort.markEnded(storeId, event.liveId(), event.occurredAt()));
    }

    /** The seller's store, if any — a seller without a store has nothing to project onto. */
    private Optional<UUID> storeIdOf(UUID sellerId) {
        return loadStorePort.loadByUserId(sellerId).map(Store::getId);
    }
}
