package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.UpdateProductUseCase.UpdateProductCommand;
import com.livecomerce.catalog.application.port.out.LoadCategoryPort;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.CategoryStatus;
import com.livecomerce.catalog.domain.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProductServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock SaveProductPort saveProductPort;
    @Mock LoadCategoryPort loadCategoryPort;

    @InjectMocks UpdateProductService service;

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();

    private static Product buildProduct() {
        return Product.create(STORE_ID, "Original Name", null, BigDecimal.TEN, "MXN", null, null);
    }

    @Test
    void update_whenValid_updatesFieldsAndReturns() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.update(
                new UpdateProductCommand(PRODUCT_ID, STORE_ID, "New Name", "desc", BigDecimal.valueOf(99), "USD", "SKU-001", null, null));

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getBasePrice()).isEqualByComparingTo(BigDecimal.valueOf(99));
    }

    @Test
    void update_whenProductNotFound_throwsProductNotFoundException() {
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                new UpdateProductCommand(PRODUCT_ID, STORE_ID, "Name", null, BigDecimal.TEN, "MXN", null, null, null)))
                .isInstanceOf(ProductNotFoundException.class);

        verify(saveProductPort, never()).save(any());
    }

    @Test
    void update_whenProductBelongsToDifferentStore_throwsAccessDeniedException() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));

        var differentStoreId = UUID.randomUUID();

        assertThatThrownBy(() -> service.update(
                new UpdateProductCommand(PRODUCT_ID, differentStoreId, "Name", null, BigDecimal.TEN, "MXN", null, null, null)))
                .isInstanceOf(AccessDeniedException.class);

        verify(saveProductPort, never()).save(any());
    }

    @Test
    void update_withCompareAtPrice_setsCompareAtPriceOnProduct() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.update(new UpdateProductCommand(
                PRODUCT_ID, STORE_ID, "New Name", "desc", BigDecimal.valueOf(75), "MXN", null, null,
                BigDecimal.valueOf(100)));

        assertThat(result.getCompareAtPrice()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void update_withNullCompareAtPrice_clearsExistingCompareAtPrice() {
        var product = buildProduct();
        product.updateCompareAtPrice(BigDecimal.valueOf(50));
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.update(new UpdateProductCommand(
                PRODUCT_ID, STORE_ID, "New Name", "desc", BigDecimal.TEN, "MXN", null, null, null));

        assertThat(result.getCompareAtPrice()).isNull();
    }

    // --- category validation ---

    @Test
    void update_withActiveCategory_assignsCategory() {
        var product = buildProduct();
        var category = Category.create("Moda Femenina", "moda-femenina", null);
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(loadCategoryPort.loadById(category.getId())).thenReturn(Optional.of(category));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.update(commandWithCategory(category.getId()));

        assertThat(result.getCategoryId()).isEqualTo(category.getId());
    }

    @Test
    void update_withUnknownCategory_throwsCategoryNotAvailable() {
        var product = buildProduct();
        var unknownId = UUID.randomUUID();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(loadCategoryPort.loadById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(commandWithCategory(unknownId)))
                .isInstanceOf(CategoryNotAvailableException.class);

        verify(saveProductPort, never()).save(any());
    }

    @Test
    void update_withPendingReviewCategory_throwsCategoryNotAvailable() {
        var product = buildProduct();
        var pending = Category.create("Pendiente", "pendiente", null);
        ReflectionTestUtils.setField(pending, "status", CategoryStatus.PENDING_REVIEW);
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(loadCategoryPort.loadById(pending.getId())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.update(commandWithCategory(pending.getId())))
                .isInstanceOf(CategoryNotAvailableException.class);

        verify(saveProductPort, never()).save(any());
    }

    @Test
    void update_withNullCategory_keepsCategoryWithoutQueryingPort() {
        var product = buildProduct();
        when(loadProductPort.loadById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(commandWithCategory(null));

        verifyNoInteractions(loadCategoryPort);
    }

    private static UpdateProductCommand commandWithCategory(UUID categoryId) {
        return new UpdateProductCommand(
                PRODUCT_ID, STORE_ID, "New Name", "desc", BigDecimal.TEN, "MXN", null, categoryId, null);
    }
}
