package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProductVariantJpaRepository extends JpaRepository<ProductVariant, UUID> {

    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.stock WHERE v.id = :id")
    Optional<ProductVariant> findByIdWithStock(@Param("id") UUID id);

    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.stock WHERE v.product.id = :productId")
    List<ProductVariant> findByProductIdWithStock(@Param("productId") UUID productId);
}
