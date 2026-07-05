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
