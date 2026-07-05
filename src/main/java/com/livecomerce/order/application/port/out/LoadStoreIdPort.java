package com.livecomerce.order.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the storeId owned by a given user, read directly from the shared
 * {@code stores} table (native/read-only entity — never importing {@code
 * store.application}), to avoid a module dependency cycle: {@code store}
 * already depends on {@code live} (implements its SPI ports) and {@code live}
 * already depends on {@code order} (implements its SPI ports), so an order
 * -> store import would close a store -> live -> order -> store cycle.
 */
public interface LoadStoreIdPort {

    Optional<UUID> findStoreIdByUserId(UUID userId);
}
