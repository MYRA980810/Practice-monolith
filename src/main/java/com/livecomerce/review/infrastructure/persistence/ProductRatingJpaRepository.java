package com.livecomerce.review.infrastructure.persistence;

import com.livecomerce.review.domain.ProductRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface ProductRatingJpaRepository extends JpaRepository<ProductRating, UUID> {

    @Query("""
            SELECT pr.productId AS productId, AVG(pr.rating) AS avgRating, COUNT(pr) AS cnt
            FROM ProductRating pr
            WHERE pr.productId IN :productIds
            GROUP BY pr.productId
            """)
    List<Object[]> aggregateByProductIds(@Param("productIds") Collection<UUID> productIds);
}
