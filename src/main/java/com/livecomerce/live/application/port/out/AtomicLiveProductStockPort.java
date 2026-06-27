package com.livecomerce.live.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for atomic stock operations on live products.
 * Implementation uses native SQL to guarantee atomicity without pessimistic locking.
 */
public interface AtomicLiveProductStockPort {

    /**
     * Atomically increments stock_sold by qty only if stock is available.
     * For hot products (is_hot = true), always succeeds.
     *
     * @return the live product id if the update succeeded, empty if stock was exhausted
     */
    Optional<UUID> atomicIncrementStockSold(UUID liveProductId, int qty);

    /**
     * Compensating operation: decrements stock_sold on purchase failure.
     */
    void decrementStockSold(UUID liveProductId, int qty);
}
