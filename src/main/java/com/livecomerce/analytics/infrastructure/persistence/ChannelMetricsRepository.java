package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.domain.OrderSalesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface ChannelMetricsRepository extends JpaRepository<OrderSalesEntity, UUID> {

    @Query(value = """
            SELECT COALESCE(SUM(CASE WHEN o.live_id IS NULL THEN oi.subtotal ELSE 0 END), 0) AS store_revenue,
                   COUNT(DISTINCT CASE WHEN o.live_id IS NULL THEN o.id END) AS store_orders,
                   COALESCE(SUM(CASE WHEN o.live_id IS NOT NULL THEN oi.subtotal ELSE 0 END), 0) AS live_revenue,
                   COUNT(DISTINCT CASE WHEN o.live_id IS NOT NULL THEN o.id END) AS live_orders
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id AND oi.status = 'PAID'
            WHERE o.store_id = :storeId
              AND o.created_at >= :from AND o.created_at < :to
            """, nativeQuery = true)
    Object[] findChannelComparison(@Param("storeId") UUID storeId,
                                    @Param("from") OffsetDateTime from,
                                    @Param("to") OffsetDateTime to);

    @Query(value = """
            SELECT l.id, l.title,
                   COALESCE(SUM(oi.subtotal), 0) AS revenue,
                   COALESCE(SUM(oi.quantity), 0) AS units_sold,
                   l.peak_viewers,
                   COALESCE(SUM(lp.stock_allocated), 0) AS total_allocated
            FROM lives l
            LEFT JOIN orders o ON o.live_id = l.id AND o.store_id = :storeId
            LEFT JOIN order_items oi ON oi.order_id = o.id AND oi.status = 'PAID'
            LEFT JOIN live_products lp ON lp.live_id = l.id
            WHERE l.store_id = :storeId AND l.status = 'ENDED'
              AND l.ended_at >= :from AND l.ended_at < :to
            GROUP BY l.id, l.title, l.peak_viewers
            ORDER BY revenue DESC
            """, nativeQuery = true)
    List<Object[]> findLivePerformances(@Param("storeId") UUID storeId,
                                         @Param("from") OffsetDateTime from,
                                         @Param("to") OffsetDateTime to);
}
