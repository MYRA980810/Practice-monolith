package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.domain.OrderItemSalesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface ProductMetricsRepository extends JpaRepository<OrderItemSalesEntity, UUID> {

    @Query(value = """
            SELECT oi.product_id, oi.product_name,
                   SUM(oi.quantity) AS units_sold,
                   SUM(oi.subtotal) AS total_revenue
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            LEFT JOIN products p ON p.id = oi.product_id
            WHERE o.store_id = :storeId AND oi.status = 'PAID'
              AND oi.paid_at >= :from AND oi.paid_at < :to
              AND (:categoryId IS NULL OR p.category_id = :categoryId)
            GROUP BY oi.product_id, oi.product_name
            ORDER BY units_sold DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopByUnits(@Param("storeId") UUID storeId,
                                   @Param("categoryId") UUID categoryId,
                                   @Param("limit") int limit,
                                   @Param("from") OffsetDateTime from,
                                   @Param("to") OffsetDateTime to);

    @Query(value = """
            SELECT oi.product_id, oi.product_name,
                   SUM(oi.quantity) AS units_sold,
                   SUM(oi.subtotal) AS total_revenue
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            LEFT JOIN products p ON p.id = oi.product_id
            WHERE o.store_id = :storeId AND oi.status = 'PAID'
              AND oi.paid_at >= :from AND oi.paid_at < :to
              AND (:categoryId IS NULL OR p.category_id = :categoryId)
            GROUP BY oi.product_id, oi.product_name
            ORDER BY total_revenue DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopByRevenue(@Param("storeId") UUID storeId,
                                     @Param("categoryId") UUID categoryId,
                                     @Param("limit") int limit,
                                     @Param("from") OffsetDateTime from,
                                     @Param("to") OffsetDateTime to);
}
