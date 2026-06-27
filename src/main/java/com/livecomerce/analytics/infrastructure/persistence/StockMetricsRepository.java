package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.domain.ProductReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface StockMetricsRepository extends JpaRepository<ProductReadEntity, UUID> {

    @Query(value = """
            SELECT p.id, p.name,
                   COALESCE(SUM(s.available_quantity), 0) AS available_qty,
                   COALESCE(SUM(s.available_quantity * COALESCE(pv.price_override, p.base_price)), 0) AS stock_value,
                   MAX(oi.paid_at) AS last_sold_at
            FROM products p
            JOIN product_variants pv ON pv.product_id = p.id
            JOIN stocks s ON s.variant_id = pv.id
            LEFT JOIN order_items oi ON oi.product_id = p.id AND oi.status = 'PAID'
            WHERE p.store_id = :storeId AND p.is_active = true
            GROUP BY p.id, p.name
            HAVING MAX(oi.paid_at) IS NULL
               OR MAX(oi.paid_at) < (NOW() - make_interval(days => :notSoldInDays))
            ORDER BY stock_value DESC
            """, nativeQuery = true)
    List<Object[]> findDeadStock(@Param("storeId") UUID storeId,
                                  @Param("notSoldInDays") int notSoldInDays);

    @Query(value = """
            SELECT p.id AS product_id, p.name AS product_name,
                   pv.id AS variant_id, pv.sku,
                   s.available_quantity,
                   CASE WHEN s.available_quantity = 0 THEN 'OUT_OF_STOCK' ELSE 'LOW_STOCK' END AS alert_type
            FROM products p
            JOIN product_variants pv ON pv.product_id = p.id
            JOIN stocks s ON s.variant_id = pv.id
            WHERE p.store_id = :storeId AND p.is_active = true
              AND s.available_quantity < 5
            ORDER BY s.available_quantity ASC
            """, nativeQuery = true)
    List<Object[]> findStockAlerts(@Param("storeId") UUID storeId);
}
