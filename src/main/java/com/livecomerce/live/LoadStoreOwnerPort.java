package com.livecomerce.live;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the seller who owns a store. Lives belong to the seller, and a store is the
 * seller's optional catalog channel, so a store's lives are its owner's lives. Implemented
 * by the {@code store} module so {@code live} never depends on it.
 */
public interface LoadStoreOwnerPort {

    Optional<UUID> loadOwnerId(UUID storeId);
}
