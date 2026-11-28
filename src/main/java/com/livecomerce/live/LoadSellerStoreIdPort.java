package com.livecomerce.live;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the store a seller owns (0..1 per seller), the inverse of {@link LoadStoreOwnerPort}.
 * Implemented by the {@code store} module so {@code live} never depends on it.
 */
public interface LoadSellerStoreIdPort {

    /**
     * The seller's store regardless of its state. Used to attribute live orders to the
     * seller's store until orders carry the seller directly.
     */
    Optional<UUID> findStoreIdBySellerId(UUID sellerId);

    /**
     * The seller's store only if it is open for sales (active, not suspended and not
     * temporarily closed); empty if the seller has no store or it is not open. Gates
     * selling catalog products during a live.
     */
    Optional<UUID> findOpenStoreIdBySellerId(UUID sellerId);
}
