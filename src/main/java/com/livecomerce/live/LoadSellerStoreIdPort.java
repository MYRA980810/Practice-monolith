package com.livecomerce.live;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the store a seller owns (0..1 per seller), the inverse of {@link LoadStoreOwnerPort}.
 * Used to attribute live orders to the seller's store until orders carry the seller directly.
 * Implemented by the {@code store} module so {@code live} never depends on it.
 */
public interface LoadSellerStoreIdPort {

    Optional<UUID> findStoreIdBySellerId(UUID sellerId);
}
