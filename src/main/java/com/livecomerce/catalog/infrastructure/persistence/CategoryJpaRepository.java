package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CategoryJpaRepository extends JpaRepository<Category, UUID> {
    List<Category> findAllByStatus(CategoryStatus status);
    Optional<Category> findBySlug(String slug);
}
