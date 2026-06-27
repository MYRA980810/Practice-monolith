package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.domain.OrderSalesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface SalesMetricsRepository extends JpaRepository<OrderSalesEntity, UUID> {

    @Query(value = """
            SELECT COALESCE(SUM(oi.subtotal), 0) AS total_revenue,
                   COUNT(DISTINCT o.id) AS total_orders,
                   COUNT(DISTINCT CASE WHEN o.status IN ('PAID','SHIPPED','DELIVERED') THEN o.id END) AS paid_orders,
                   COUNT(DISTINCT CASE WHEN o.status = 'CANCELLED' THEN o.id END) AS cancelled_orders
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id AND oi.status = 'PAID'
            WHERE o.store_id = :storeId
              AND o.created_at >= :from AND o.created_at < :to
            """, nativeQuery = true)
    Object[] findSalesSummary(@Param("storeId") UUID storeId,
                              @Param("from") OffsetDateTime from,
                              @Param("to") OffsetDateTime to);

    @Query(value = """
            SELECT COUNT(*) FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE o.store_id = :storeId
              AND o.created_at >= :from AND o.created_at < :to
            """, nativeQuery = true)
    long countTotalItems(@Param("storeId") UUID storeId,
                         @Param("from") OffsetDateTime from,
                         @Param("to") OffsetDateTime to);

    @Query(value = """
            SELECT COUNT(*) FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE o.store_id = :storeId AND oi.status = 'PAID'
              AND o.created_at >= :from AND o.created_at < :to
            """, nativeQuery = true)
    long countPaidItems(@Param("storeId") UUID storeId,
                        @Param("from") OffsetDateTime from,
                        @Param("to") OffsetDateTime to);

    @Query(value = """
            SELECT date_trunc(:period, oi.paid_at) AS period_bucket,
                   COALESCE(SUM(oi.subtotal), 0) AS revenue,
                   COUNT(DISTINCT o.id) AS order_count
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE o.store_id = :storeId AND oi.status = 'PAID'
              AND oi.paid_at >= :from AND oi.paid_at < :to
            GROUP BY period_bucket
            ORDER BY period_bucket
            """, nativeQuery = true)
    List<Object[]> findRevenueByPeriod(@Param("storeId") UUID storeId,
                                       @Param("period") String period,
                                       @Param("from") OffsetDateTime from,
                                       @Param("to") OffsetDateTime to);

    @Query(value = """
            SELECT p.method, COUNT(*) AS cnt, COALESCE(SUM(p.amount), 0) AS total
            FROM payments p
            JOIN orders o ON o.id = p.order_id
            WHERE o.store_id = :storeId AND p.status = 'CONFIRMED'
              AND p.confirmed_at >= :from AND p.confirmed_at < :to
            GROUP BY p.method
            """, nativeQuery = true)
    List<Object[]> findPaymentMethodBreakdown(@Param("storeId") UUID storeId,
                                               @Param("from") OffsetDateTime from,
                                               @Param("to") OffsetDateTime to);

    @Query(value = """
            SELECT COALESCE(SUM(p.amount * st.commission_rate), 0)
            FROM payments p
            JOIN orders o ON o.id = p.order_id
            JOIN stores st ON st.id = o.store_id
            WHERE o.store_id = :storeId AND p.status = 'CONFIRMED'
              AND p.confirmed_at >= :from AND p.confirmed_at < :to
            """, nativeQuery = true)
    Object findCommission(@Param("storeId") UUID storeId,
                          @Param("from") OffsetDateTime from,
                          @Param("to") OffsetDateTime to);
}
