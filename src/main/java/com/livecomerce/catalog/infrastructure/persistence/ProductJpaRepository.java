package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProductJpaRepository extends JpaRepository<Product, UUID> {

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.variants v LEFT JOIN FETCH v.stock LEFT JOIN FETCH v.optionValues WHERE p.id = :id")
    Optional<Product> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.variants v LEFT JOIN FETCH v.stock WHERE p.storeId = :storeId AND p.active = true")
    List<Product> findByStoreIdActiveWithDetails(@Param("storeId") UUID storeId);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.variants v LEFT JOIN FETCH v.optionValues WHERE p.storeId = :storeId AND p.active = true")
    List<Product> findByStoreIdActiveWithVariantOptionValues(@Param("storeId") UUID storeId);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images WHERE p.storeId = :storeId AND p.active = true")
    List<Product> findByStoreIdActiveWithImages(@Param("storeId") UUID storeId);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.options o WHERE p.storeId = :storeId AND p.active = true")
    List<Product> findByStoreIdActiveWithOptions(@Param("storeId") UUID storeId);
}
