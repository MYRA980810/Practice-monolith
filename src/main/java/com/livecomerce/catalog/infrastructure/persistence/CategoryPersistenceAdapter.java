package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.application.port.out.LoadCategoryPort;
import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.CategoryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class CategoryPersistenceAdapter implements LoadCategoryPort {

    private final CategoryJpaRepository repository;

    @Override
    @SuppressWarnings("null")
    public Optional<Category> loadById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<Category> loadAllActive() {
        return repository.findAllByStatusOrderedForDisplay(CategoryStatus.ACTIVE);
    }

    @Override
    public List<Category> loadCategoriesInUseByStore(UUID storeId) {
        return repository.findCategoriesInUseByStore(storeId);
    }
}
