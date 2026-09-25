package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.CategoryStatus;
import com.livecomerce.store.StoreCategoryPort.CategoryRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class StoreCategoryAdapterTest {

    @Mock ProductJpaRepository productRepository;
    @Mock CategoryJpaRepository categoryRepository;

    @InjectMocks StoreCategoryAdapter adapter;

    private static final UUID STORE_A = UUID.randomUUID();
    private static final UUID STORE_B = UUID.randomUUID();

    private static Category category(String name, String slug, CategoryStatus status) {
        var category = Category.create(name, slug, null);
        ReflectionTestUtils.setField(category, "status", status);
        return category;
    }

    private static Object[] row(UUID storeId, UUID categoryId, long count) {
        return new Object[]{storeId, categoryId, count};
    }

    // --- inferTopByStore ---

    @Test
    void inferTopByStore_emptyInput_returnsEmptyWithoutQuerying() {
        assertThat(adapter.inferTopByStore(Set.of())).isEmpty();

        verifyNoInteractions(productRepository, categoryRepository);
    }

    @Test
    void inferTopByStore_picksCategoryWithMostActiveProducts() {
        var moda = category("Moda", "moda", CategoryStatus.ACTIVE);
        var hogar = category("Hogar", "hogar", CategoryStatus.ACTIVE);
        when(productRepository.countActiveByStoreAndCategory(anyCollection())).thenReturn(List.of(
                row(STORE_A, moda.getId(), 2L),
                row(STORE_A, hogar.getId(), 5L)));
        when(categoryRepository.findAllById(any())).thenReturn(List.of(moda, hogar));

        var result = adapter.inferTopByStore(Set.of(STORE_A));

        assertThat(result).containsExactlyEntriesOf(
                Map.of(STORE_A, new CategoryRef(hogar.getId(), "Hogar", "hogar")));
    }

    @Test
    void inferTopByStore_tie_resolvesToLowestCategoryUuid() {
        var low = category("Low", "low", CategoryStatus.ACTIVE);
        var high = category("High", "high", CategoryStatus.ACTIVE);
        ReflectionTestUtils.setField(low, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        ReflectionTestUtils.setField(high, "id", UUID.fromString("00000000-0000-0000-0000-000000000009"));
        when(productRepository.countActiveByStoreAndCategory(anyCollection())).thenReturn(List.of(
                row(STORE_A, high.getId(), 3L),
                row(STORE_A, low.getId(), 3L)));
        when(categoryRepository.findAllById(any())).thenReturn(List.of(high, low));

        var result = adapter.inferTopByStore(Set.of(STORE_A));

        assertThat(result.get(STORE_A).id()).isEqualTo(low.getId());
    }

    @Test
    void inferTopByStore_storeWithoutCategorizedProducts_isAbsent() {
        var moda = category("Moda", "moda", CategoryStatus.ACTIVE);
        when(productRepository.countActiveByStoreAndCategory(anyCollection())).thenReturn(List.<Object[]>of(
                row(STORE_A, moda.getId(), 1L)));
        when(categoryRepository.findAllById(any())).thenReturn(List.of(moda));

        var result = adapter.inferTopByStore(Set.of(STORE_A, STORE_B));

        assertThat(result).containsOnlyKeys(STORE_A);
    }

    @Test
    void inferTopByStore_winningCategoryInactive_fallsBackToNextBestActive() {
        var pending = category("Pending", "pending", CategoryStatus.PENDING_REVIEW);
        var moda = category("Moda", "moda", CategoryStatus.ACTIVE);
        when(productRepository.countActiveByStoreAndCategory(anyCollection())).thenReturn(List.of(
                row(STORE_A, pending.getId(), 10L),
                row(STORE_A, moda.getId(), 1L)));
        when(categoryRepository.findAllById(any())).thenReturn(List.of(pending, moda));

        var result = adapter.inferTopByStore(Set.of(STORE_A));

        assertThat(result.get(STORE_A).id()).isEqualTo(moda.getId());
    }

    @Test
    void inferTopByStore_onlyInactiveCategories_storeIsAbsent() {
        var pending = category("Pending", "pending", CategoryStatus.PENDING_REVIEW);
        when(productRepository.countActiveByStoreAndCategory(anyCollection())).thenReturn(List.<Object[]>of(
                row(STORE_A, pending.getId(), 4L)));
        when(categoryRepository.findAllById(any())).thenReturn(List.of(pending));

        assertThat(adapter.inferTopByStore(Set.of(STORE_A))).isEmpty();
    }

    @Test
    void inferTopByStore_noRows_skipsCategoryQuery() {
        when(productRepository.countActiveByStoreAndCategory(anyCollection())).thenReturn(List.of());

        assertThat(adapter.inferTopByStore(Set.of(STORE_A))).isEmpty();
        verifyNoInteractions(categoryRepository);
    }

    // --- loadActiveByIds ---

    @Test
    void loadActiveByIds_emptyInput_returnsEmptyWithoutQuerying() {
        assertThat(adapter.loadActiveByIds(Set.of())).isEmpty();

        verifyNoInteractions(categoryRepository);
    }

    @Test
    void loadActiveByIds_excludesInactiveCategories() {
        var moda = category("Moda", "moda", CategoryStatus.ACTIVE);
        var pending = category("Pending", "pending", CategoryStatus.PENDING_REVIEW);
        when(categoryRepository.findAllById(any())).thenReturn(List.of(moda, pending));

        var result = adapter.loadActiveByIds(Set.of(moda.getId(), pending.getId()));

        assertThat(result).containsExactlyEntriesOf(
                Map.of(moda.getId(), new CategoryRef(moda.getId(), "Moda", "moda")));
    }

    // --- isActive ---

    @Test
    void isActive_activeCategory_returnsTrue() {
        var moda = category("Moda", "moda", CategoryStatus.ACTIVE);
        when(categoryRepository.findById(moda.getId())).thenReturn(Optional.of(moda));

        assertThat(adapter.isActive(moda.getId())).isTrue();
    }

    @Test
    void isActive_inactiveOrMissingCategory_returnsFalse() {
        var pending = category("Pending", "pending", CategoryStatus.PENDING_REVIEW);
        var missing = UUID.randomUUID();
        when(categoryRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(categoryRepository.findById(missing)).thenReturn(Optional.empty());

        assertThat(adapter.isActive(pending.getId())).isFalse();
        assertThat(adapter.isActive(missing)).isFalse();
    }
}
