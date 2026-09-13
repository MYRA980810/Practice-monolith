package com.livecomerce.review.infrastructure.persistence;

import com.livecomerce.review.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface ReviewJpaRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByStoreId(UUID storeId, Pageable pageable);

    @Query("""
            SELECT r.storeId AS storeId, AVG(r.rankingImpactScore) AS avgScore, COUNT(r) AS cnt
            FROM Review r
            WHERE r.storeId IN :storeIds
            GROUP BY r.storeId
            """)
    List<Object[]> aggregateByStoreIds(@Param("storeIds") Collection<UUID> storeIds);

    @Query("""
            SELECT DISTINCT r FROM Review r
            JOIN r.productRatings pr
            WHERE pr.productId = :productId
            """)
    Page<Review> findByProductId(@Param("productId") UUID productId, Pageable pageable);
}
