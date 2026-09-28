package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface CategoryJpaRepository extends JpaRepository<Category, UUID> {
    @Query("SELECT c FROM Category c WHERE c.status = :status " +
           "ORDER BY c.displayOrder ASC NULLS LAST, c.name ASC")
    List<Category> findAllByStatusOrderedForDisplay(@Param("status") CategoryStatus status);


    @Query("SELECT DISTINCT c FROM Category c WHERE c.id IN " +
           "(SELECT DISTINCT p.categoryId FROM Product p WHERE p.storeId = :storeId AND p.active = true AND p.paused = false)")
    List<Category> findCategoriesInUseByStore(@Param("storeId") UUID storeId);
}
