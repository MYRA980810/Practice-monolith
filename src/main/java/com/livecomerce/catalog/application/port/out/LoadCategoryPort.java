package com.livecomerce.catalog.application.port.out;

import com.livecomerce.catalog.domain.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadCategoryPort {
    Optional<Category> loadById(UUID id);
    List<Category> loadAllActive();
    Optional<Category> findBySlug(String slug);
    List<Category> loadCategoriesInUseByStore(UUID storeId);
}
