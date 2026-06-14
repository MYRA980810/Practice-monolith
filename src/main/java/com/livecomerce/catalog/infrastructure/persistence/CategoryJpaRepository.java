package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CategoryJpaRepository extends JpaRepository<Category, UUID> {
    List<Category> findAllByStatus(CategoryStatus status);
    Optional<Category> findBySlug(String slug);

    @Query("SELECT DISTINCT c FROM Category c WHERE c.id IN " +
           "(SELECT DISTINCT p.categoryId FROM Product p WHERE p.storeId = :storeId AND p.active = true)")
    List<Category> findCategoriesInUseByStore(@Param("storeId") UUID storeId);
}
