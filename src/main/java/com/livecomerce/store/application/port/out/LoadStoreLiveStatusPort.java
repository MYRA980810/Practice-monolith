package com.livecomerce.store.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Reads the local {@code store_live_status} projection kept current by {@link
 * com.livecomerce.store.application.StoreLiveStatusEventListener} — never a live
 * cross-module read at request time (see that listener's javadoc for why).
 */
public interface LoadStoreLiveStatusPort {

    /**
     * Maps storeId to its currently active liveId. A store absent from the
     * result has no active live right now.
     */
    Map<UUID, UUID> loadActiveLiveIds(Collection<UUID> storeIds);
}
