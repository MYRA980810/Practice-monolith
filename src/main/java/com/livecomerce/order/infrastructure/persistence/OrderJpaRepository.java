package com.livecomerce.order.infrastructure.persistence;

import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemStatus;
import com.livecomerce.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OrderJpaRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);

    /**
     * Live-scoped lookup: {@code liveSessionId} MUST NOT be {@code null}.
     * The {@code = :liveSessionId} comparison relies on SQL equality
     * semantics, which never match {@code NULL} (a null-safe {@code IS NOT
     * DISTINCT FROM} would be needed for that). Its only real caller is
     * {@code PlaceOrderItemService}, which always passes a non-null
     * {@code liveSessionId} on the live-purchase path — the batched cart
     * checkout path ({@code PlaceCartOrderService}) never calls this query
     * at all, since a cart checkout always opens a fresh Order per store
     * rather than reusing one. Left unchanged deliberately: "fixing" the
     * null comparison here would be dead code for this query's only caller
     * and an unnecessary risk to the live-purchase path.
     */
    @Query("""
            SELECT o FROM Order o LEFT JOIN FETCH o.items
            WHERE o.buyerId = :buyerId
              AND o.liveSessionId = :liveSessionId
              AND o.status = :status
            """)
    Optional<Order> findActiveByBuyerAndLive(
            @Param("buyerId") UUID buyerId,
            @Param("liveSessionId") UUID liveSessionId,
            @Param("status") OrderStatus status);

    @Query("""
            SELECT o FROM Order o LEFT JOIN FETCH o.items
            WHERE o.storeId = :storeId
              AND o.liveSessionId = :liveId
              AND o.status = :status
            """)
    List<Order> findReadyToShipByLive(
            @Param("storeId") UUID storeId,
            @Param("liveId") UUID liveId,
            @Param("status") OrderStatus status);

    @Query("""
            SELECT DISTINCT o FROM Order o JOIN FETCH o.items i
            WHERE i.status = :itemStatus
              AND i.reservedUntil < CURRENT_TIMESTAMP
            """)
    List<Order> findOrdersWithExpiredReservations(@Param("itemStatus") OrderItemStatus itemStatus);
}
