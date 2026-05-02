package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProductJpaRepository extends JpaRepository<Product, UUID> {

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.stock LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.stock LEFT JOIN FETCH p.images WHERE p.storeId = :storeId AND p.active = true")
    List<Product> findByStoreIdActiveWithDetails(@Param("storeId") UUID storeId);
}
