package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.CreateProductUseCase.CreateProductCommand;
import com.livecomerce.catalog.application.port.in.CreateProductUseCase.ImageData;
import com.livecomerce.catalog.application.port.out.LoadCategoryPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.CategoryStatus;
import com.livecomerce.catalog.domain.Product;
import com.livecomerce.catalog.domain.ProductCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProductServiceTest {

    @Mock SaveProductPort saveProductPort;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock LoadCategoryPort loadCategoryPort;

    @InjectMocks CreateProductService service;

    private static final UUID STORE_ID = UUID.randomUUID();
    private static final Category ACTIVE_CATEGORY = Category.create("Moda Femenina", "moda-femenina", null);
    private static final UUID CATEGORY_ID = ACTIVE_CATEGORY.getId();

    private static final CreateProductCommand VALID_COMMAND = new CreateProductCommand(
            STORE_ID, "Remera Básica", "Remera de algodón", new BigDecimal("150.00"), "MXN", "SKU-001", CATEGORY_ID, null
    );

    private static Product buildProduct() {
        return Product.create(STORE_ID, "Remera Básica", "Remera de algodón",
                new BigDecimal("150.00"), "MXN", "SKU-001", null);
    }

    @BeforeEach
    void stubActiveCategory() {
        lenient().when(loadCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.of(ACTIVE_CATEGORY));
    }

    @Test
    void create_withValidCommand_savesProductAndReturnsIt() {
        var saved = buildProduct();
        when(saveProductPort.save(any())).thenReturn(saved);

        var result = service.create(VALID_COMMAND);

        assertThat(result.getName()).isEqualTo("Remera Básica");
        assertThat(result.getStoreId()).isEqualTo(STORE_ID);
        assertThat(result.defaultVariant().getSku()).isEqualTo("SKU-001");
    }

    @Test
    @SuppressWarnings("null")
    void create_publishesProductCreatedEvent() {
        var saved = buildProduct();
        when(saveProductPort.save(any())).thenReturn(saved);

        service.create(VALID_COMMAND);

        var captor = ArgumentCaptor.forClass(ProductCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().storeId()).isEqualTo(STORE_ID);
        assertThat(captor.getValue().name()).isEqualTo("Remera Básica");
    }

    @Test
    void create_newProductHasEmptyStock() {
        var saved = buildProduct();
        when(saveProductPort.save(any())).thenReturn(saved);

        service.create(VALID_COMMAND);

        var captor = ArgumentCaptor.forClass(Product.class);
        verify(saveProductPort).save(captor.capture());
        var stock = captor.getValue().defaultVariant().getStock();
        assertThat(stock.getTotalQuantity()).isEqualTo(0);
        assertThat(stock.getAvailableQuantity()).isEqualTo(0);
        assertThat(stock.getReservedQuantity()).isEqualTo(0);
    }

    @Test
    void create_withNullCurrency_defaultsToMXN() {
        var command = new CreateProductCommand(STORE_ID, "Producto", null, BigDecimal.TEN, null, null, CATEGORY_ID, null);
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.create(command);

        assertThat(result.getCurrency()).isEqualTo("MXN");
    }

    @Test
    void create_withThreeImages_savedProductHasThreeImages() {
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var images = List.of(
                new ImageData("https://cdn.example.com/1.jpg", 0, true),
                new ImageData("https://cdn.example.com/2.jpg", 1, false),
                new ImageData("https://cdn.example.com/3.jpg", 2, false)
        );
        var command = new CreateProductCommand(
                STORE_ID, "Remera", null, new BigDecimal("100.00"), "MXN", null, CATEGORY_ID, images);

        service.create(command);

        var captor = ArgumentCaptor.forClass(Product.class);
        verify(saveProductPort).save(captor.capture());
        assertThat(captor.getValue().getImages()).hasSize(3);
    }

    @Test
    void create_withNullImages_savedProductHasNoImages() {
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var command = new CreateProductCommand(
                STORE_ID, "Remera", null, new BigDecimal("100.00"), "MXN", null, CATEGORY_ID, null);

        service.create(command);

        var captor = ArgumentCaptor.forClass(Product.class);
        verify(saveProductPort).save(captor.capture());
        assertThat(captor.getValue().getImages()).isEmpty();
    }

    @Test
    void create_withEmptyImages_savedProductHasNoImages() {
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var command = new CreateProductCommand(
                STORE_ID, "Remera", null, new BigDecimal("100.00"), "MXN", null, CATEGORY_ID, List.of());

        service.create(command);

        var captor = ArgumentCaptor.forClass(Product.class);
        verify(saveProductPort).save(captor.capture());
        assertThat(captor.getValue().getImages()).isEmpty();
    }

    // --- category validation ---

    @Test
    void create_withActiveCategory_assignsCategory() {
        when(saveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.create(VALID_COMMAND);

        assertThat(result.getCategoryId()).isEqualTo(CATEGORY_ID);
    }

    @Test
    void create_withUnknownCategory_throwsCategoryNotAvailable() {
        var unknownId = UUID.randomUUID();
        when(loadCategoryPort.loadById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(commandWithCategory(unknownId)))
                .isInstanceOf(CategoryNotAvailableException.class);

        verify(saveProductPort, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void create_withPendingReviewCategory_throwsCategoryNotAvailable() {
        var pending = Category.create("Pendiente", "pendiente", null);
        ReflectionTestUtils.setField(pending, "status", CategoryStatus.PENDING_REVIEW);
        when(loadCategoryPort.loadById(pending.getId())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.create(commandWithCategory(pending.getId())))
                .isInstanceOf(CategoryNotAvailableException.class);

        verify(saveProductPort, never()).save(any());
    }

    private static CreateProductCommand commandWithCategory(UUID categoryId) {
        return new CreateProductCommand(
                STORE_ID, "Remera", null, new BigDecimal("100.00"), "MXN", null, categoryId, null);
    }
}
