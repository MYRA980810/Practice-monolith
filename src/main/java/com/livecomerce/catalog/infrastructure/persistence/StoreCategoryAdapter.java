package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.CategoryStatus;
import com.livecomerce.store.StoreCategoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StoreCategoryAdapter implements StoreCategoryPort {

    // Highest product count wins; ties resolve to the lowest category UUID (deterministic).
    private static final Comparator<CategoryCount> WINNER_ORDER =
            Comparator.comparingLong(CategoryCount::count).reversed()
                    .thenComparing(CategoryCount::categoryId);

    private final ProductJpaRepository productRepository;
    private final CategoryJpaRepository categoryRepository;

    private record CategoryCount(UUID categoryId, long count) {}

    @Override
    public Map<UUID, CategoryRef> inferTopByStore(Collection<UUID> storeIds) {
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        var rows = productRepository.countActiveByStoreAndCategory(storeIds);
        if (rows.isEmpty()) {
            return Map.of();
        }

        Map<UUID, List<CategoryCount>> countsByStore = new HashMap<>();
        var categoryIds = new HashSet<UUID>();
        for (Object[] row : rows) {
            var storeId = (UUID) row[0];
            var categoryId = (UUID) row[1];
            var count = ((Number) row[2]).longValue();
            countsByStore.computeIfAbsent(storeId, k -> new ArrayList<>()).add(new CategoryCount(categoryId, count));
            categoryIds.add(categoryId);
        }

        // Inactive categories are dropped here, so a store whose top category is inactive
        // falls through to its next-best active one (or is absent if none is active).
        var activeCategories = loadActiveByIds(categoryIds);

        Map<UUID, CategoryRef> result = new HashMap<>();
        countsByStore.forEach((storeId, counts) -> counts.stream()
                .filter(c -> activeCategories.containsKey(c.categoryId()))
                .min(WINNER_ORDER)
                .ifPresent(winner -> result.put(storeId, activeCategories.get(winner.categoryId()))));
        return result;
    }

    @Override
    public Map<UUID, CategoryRef> loadActiveByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return categoryRepository.findAllById(ids).stream()
                .filter(category -> category.getStatus() == CategoryStatus.ACTIVE)
                .collect(Collectors.toMap(Category::getId, StoreCategoryAdapter::toRef, (a, b) -> a));
    }

    @Override
    @SuppressWarnings("null") // UUID is non-null by contract; JPA findById expects @NonNull
    public boolean isActive(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .map(category -> category.getStatus() == CategoryStatus.ACTIVE)
                .orElse(false);
    }

    private static CategoryRef toRef(Category category) {
        return new CategoryRef(category.getId(), category.getName(), category.getSlug());
    }
}
