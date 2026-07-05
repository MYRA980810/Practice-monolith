package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.domain.OrderSalesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Batched, read-time buyer display-name lookup against the shared {@code
 * users} table. No {@code auth.domain} import — native query bypasses the
 * JpaRepository entity type, reusing {@link OrderSalesEntity} purely as the
 * generic carrier (same established smell as {@link LiveSummaryOrderDetailRepository}).
 */
interface BuyerNameRepository extends JpaRepository<OrderSalesEntity, UUID> {

    @Query(value = "SELECT id, first_name, last_name FROM users WHERE id IN (:buyerIds)", nativeQuery = true)
    List<Object[]> findNamesByIds(@Param("buyerIds") Collection<UUID> buyerIds);
}
