package com.livecomerce.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface LiveProductImageReadRepository extends JpaRepository<LiveProductImageReadEntity, UUID> {

    @Query("""
            SELECT lp.productId, lp.imageUrl FROM LiveProductImageReadEntity lp
            WHERE lp.liveId = :liveId AND lp.productId IN :productIds
            """)
    List<Object[]> findImageUrlsByLiveAndProductIds(
            @Param("liveId") UUID liveId,
            @Param("productIds") Collection<UUID> productIds);
}
