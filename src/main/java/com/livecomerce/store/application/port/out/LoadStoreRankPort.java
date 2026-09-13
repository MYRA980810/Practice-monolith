package com.livecomerce.store.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves each store's current position from the latest {@code store_ranking_snapshots}
 * batch, read directly off that shared table (native/read-only entity — never importing
 * {@code analytics.application}), to avoid a module dependency cycle: {@code analytics}
 * already declares {@code store::in} as an allowed dependency (it consumes store's inbound
 * ports to compute the ranking), so a reverse {@code store -> analytics} import would close
 * a cycle. Same pattern as {@code order.application.port.out.LoadStoreIdPort} reading the
 * {@code stores} table without importing {@code store.application}.
 */
public interface LoadStoreRankPort {

    Map<UUID, Integer> loadRanks(Collection<UUID> storeIds);
}
