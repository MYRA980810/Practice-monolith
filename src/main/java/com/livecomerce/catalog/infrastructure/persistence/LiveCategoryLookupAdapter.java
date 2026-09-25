package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.CategoryStatus;
import com.livecomerce.live.CategoryLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class LiveCategoryLookupAdapter implements CategoryLookupPort {

    private final CategoryJpaRepository repository;

    @Override
    @SuppressWarnings("null") // UUID is non-null by contract; JPA findById expects @NonNull
    public boolean isActive(UUID categoryId) {
        return repository.findById(categoryId)
                .map(category -> category.getStatus() == CategoryStatus.ACTIVE)
                .orElse(false);
    }
}
