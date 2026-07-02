package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.domain.LiveSubscription;
import com.livecomerce.live.domain.LiveSubscriptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface LiveSubscriptionJpaRepository extends JpaRepository<LiveSubscription, LiveSubscriptionId> {

    /**
     * Bulk delete all subscriptions for a given live. Used when a live starts or is
     * cancelled to clean up subscriptions after notifications are sent.
     */
    @Modifying
    @Query("DELETE FROM LiveSubscription s WHERE s.id.liveId = :liveId")
    void deleteByLiveId(@Param("liveId") UUID liveId);

    /**
     * Idempotent delete for a single subscriber. Returns without error if the row
     * does not exist.
     */
    @Modifying
    @Query("DELETE FROM LiveSubscription s WHERE s.id.liveId = :liveId AND s.id.buyerId = :buyerId")
    void deleteByLiveIdAndBuyerId(@Param("liveId") UUID liveId, @Param("buyerId") UUID buyerId);

    @Query("SELECT s.id.buyerId FROM LiveSubscription s WHERE s.id.liveId = :liveId")
    List<UUID> findBuyerIdsByLiveId(@Param("liveId") UUID liveId);
}
