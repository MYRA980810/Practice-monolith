package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.domain.LiveProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface LiveProductJpaRepository extends JpaRepository<LiveProduct, UUID> {

    List<LiveProduct> findByLiveId(UUID liveId);

    @Query("SELECT lp FROM LiveProduct lp WHERE lp.live.id = :liveId AND lp.status = com.livecomerce.live.domain.LiveProductStatus.PINNED")
    Optional<LiveProduct> findPinnedByLiveId(@Param("liveId") UUID liveId);

    /**
     * Atomically increments stock_sold only if there is available stock.
     * For hot products (is_hot = true), always succeeds regardless of stock.
     * Returns the updated row's id if successful, empty if stock was exhausted.
     */
    @Modifying
    @Query(nativeQuery = true, value =
            "UPDATE live_products SET stock_sold = stock_sold + :qty " +
            "WHERE id = :id AND (is_hot = true OR stock_sold + :qty <= stock_allocated) " +
            "RETURNING id")
    Optional<UUID> atomicIncrementStockSold(@Param("id") UUID id, @Param("qty") int qty);

    /**
     * Compensating update: decrements stock_sold on purchase failure.
     * Uses GREATEST(0, ...) to avoid negative values.
     */
    @Modifying
    @Query(nativeQuery = true, value =
            "UPDATE live_products SET stock_sold = GREATEST(0, stock_sold - :qty) WHERE id = :id")
    void decrementStockSold(@Param("id") UUID id, @Param("qty") int qty);
}
