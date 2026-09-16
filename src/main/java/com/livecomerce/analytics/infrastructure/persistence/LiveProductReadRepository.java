package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.domain.LiveProductReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Read-only access to {@code live_products} for the frozen-snapshot
 * {@code totalAllocated} denominator (allocation set once when a product is
 * added to the live, never fluctuates with order lifecycle).
 */
interface LiveProductReadRepository extends JpaRepository<LiveProductReadEntity, UUID> {

    @Query("SELECT COALESCE(SUM(lp.stockAllocated), 0) FROM LiveProductReadEntity lp WHERE lp.liveId = :liveId")
    long sumStockAllocatedByLiveId(@Param("liveId") UUID liveId);

    /**
     * Feeds {@code catalog.LoadLiveProductStatusPort}. A native join (not a JPQL
     * relationship — {@link LiveProductReadEntity} and {@link com.livecomerce.analytics.domain.LiveReadEntity}
     * are flat, unrelated read mappings) over the two tables {@code analytics}
     * already reads directly, same style as {@code ProductMetricsRepository}'s
     * order_items/orders joins. Implementing this in {@code analytics} — rather
     * than in {@code live} — is deliberate: {@code live} cannot implement a
     * {@code catalog}-owned port without cycling back through {@code catalog ->
     * store -> live}, since {@code catalog} already depends on {@code store} and
     * {@code store} already depends on {@code live}. {@code analytics} has no
     * incoming edges from any of those three modules, so it is a safe sink for
     * this cross-module read.
     */
    @Query(value = """
            SELECT lp.product_id, lp.status, l.status
            FROM live_products lp
            JOIN lives l ON l.id = lp.live_id
            WHERE lp.product_id IN (:productIds)
              AND l.status IN ('LIVE', 'RECONNECTING')
            """, nativeQuery = true)
    List<Object[]> findActiveLiveRowsByProductIds(@Param("productIds") Collection<UUID> productIds);
}
