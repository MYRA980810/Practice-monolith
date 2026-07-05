package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.domain.OrderSalesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface LiveSummaryOrderDetailRepository extends JpaRepository<OrderSalesEntity, UUID> {

    @Query(value = """
            SELECT o.id, o.buyer_id,
                   COALESCE(SUM(oi.subtotal), 0)      AS order_total,
                   string_agg(oi.product_name, ', ')  AS item_names,
                   COALESCE(SUM(oi.quantity), 0)      AS units_sold
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id AND oi.status = 'PAID'
            WHERE o.live_id = :liveId AND o.status = 'PAID'
            GROUP BY o.id, o.buyer_id
            """, nativeQuery = true)
    List<Object[]> findQualifyingOrderDetails(@Param("liveId") UUID liveId);
}
