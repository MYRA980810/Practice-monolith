package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.application.port.out.LoadCategoryPort;
import com.livecomerce.catalog.application.port.out.SaveCategoryPort;
import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.CategoryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class CategoryPersistenceAdapter implements LoadCategoryPort, SaveCategoryPort {

    private final CategoryJpaRepository repository;

    @Override
    @SuppressWarnings("null")
    public Optional<Category> loadById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<Category> loadAllActive() {
        return repository.findAllByStatus(CategoryStatus.ACTIVE);
    }

    @Override
    public Optional<Category> findBySlug(String slug) {
        return repository.findBySlug(slug);
    }

    @Override
    public List<Category> loadCategoriesInUseByStore(UUID storeId) {
        return repository.findCategoriesInUseByStore(storeId);
    }

    @Override
    @SuppressWarnings("null")
    public Category save(Category category) {
        return repository.save(category);
    }
}
