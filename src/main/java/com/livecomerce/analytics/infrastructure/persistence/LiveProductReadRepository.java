package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.domain.LiveProductReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Read-only access to {@code live_products} for the frozen-snapshot
 * {@code totalAllocated} denominator (allocation set once when a product is
 * added to the live, never fluctuates with order lifecycle).
 */
interface LiveProductReadRepository extends JpaRepository<LiveProductReadEntity, UUID> {

    @Query("SELECT COALESCE(SUM(lp.stockAllocated), 0) FROM LiveProductReadEntity lp WHERE lp.liveId = :liveId")
    long sumStockAllocatedByLiveId(@Param("liveId") UUID liveId);
}
