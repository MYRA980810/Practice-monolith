package com.livecomerce.catalog.api;

import com.livecomerce.catalog.application.port.in.AddProductImageUseCase;
import com.livecomerce.catalog.application.port.in.AddProductOptionUseCase;
import com.livecomerce.catalog.application.port.in.AddStockUseCase;
import com.livecomerce.catalog.application.port.in.CreateProductUseCase;
import com.livecomerce.catalog.application.port.in.CreateProductVariantUseCase;
import com.livecomerce.catalog.application.port.in.DeactivateProductUseCase;
import com.livecomerce.catalog.application.port.in.GetProductUseCase;
import com.livecomerce.catalog.application.port.in.ListCategoriesUseCase;
import com.livecomerce.catalog.application.port.in.ProductFilter;
import com.livecomerce.catalog.application.port.in.ProductFilter.SortBy;
import com.livecomerce.catalog.application.port.in.ProductFilter.StockLevel;
import com.livecomerce.catalog.application.port.in.RemoveProductImageUseCase;
import com.livecomerce.catalog.application.port.in.UpdateProductImageUseCase;
import com.livecomerce.catalog.application.port.in.UpdateProductUseCase;
import com.livecomerce.catalog.application.query.ProductView;
import com.livecomerce.catalog.application.query.VariantView;
import com.livecomerce.shared.UserPrincipal;
import com.livecomerce.store.application.port.in.GetStoreUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final GetProductUseCase getProductUseCase;
    private final ListCategoriesUseCase listCategoriesUseCase;
    private final AddStockUseCase addStockUseCase;
    private final AddProductImageUseCase addProductImageUseCase;
    private final UpdateProductImageUseCase updateProductImageUseCase;
    private final RemoveProductImageUseCase removeProductImageUseCase;
    private final DeactivateProductUseCase deactivateProductUseCase;
    private final GetStoreUseCase getStoreUseCase;
    private final AddProductOptionUseCase addProductOptionUseCase;
    private final CreateProductVariantUseCase createProductVariantUseCase;

    private static final String DEFAULT_CURRENCY = "MXN";

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<ProductView> create(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        var product = createProductUseCase.create(new CreateProductUseCase.CreateProductCommand(
                storeId,
                request.name(),
                request.description(),
                request.basePrice(),
                Objects.requireNonNullElse(request.currency(), DEFAULT_CURRENCY),
                request.sku(),
                request.categoryId()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(getProductUseCase.getById(product.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<ProductView> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        updateProductUseCase.update(new UpdateProductUseCase.UpdateProductCommand(
                id,
                storeId,
                request.name(),
                request.description(),
                request.basePrice(),
                Objects.requireNonNullElse(request.currency(), DEFAULT_CURRENCY),
                request.sku(),
                request.categoryId()
        ));
        return ResponseEntity.ok(getProductUseCase.getById(id));
    }

    @GetMapping("/{id}")
    ResponseEntity<ProductView> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(getProductUseCase.getById(id));
    }

    @GetMapping
    ResponseEntity<List<ProductView>> getByStore(@RequestParam UUID storeId) {
        return ResponseEntity.ok(getProductUseCase.getByStoreId(storeId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<List<ProductView>> getMyProducts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false, defaultValue = "RECENTLY_ADDED") SortBy sort,
            @RequestParam(required = false, defaultValue = "ALL") StockLevel stockLevel) {
        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        return ResponseEntity.ok(getProductUseCase.listWithFilters(
                new ProductFilter(storeId, categoryId, sort, stockLevel)));
    }

    @GetMapping("/me/categories")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<List<CategoryResponse>> getMyCategories(@AuthenticationPrincipal UserPrincipal principal) {
        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        return ResponseEntity.ok(listCategoriesUseCase.getCategoriesInUse(storeId).stream()
                .map(CategoryResponse::from)
                .toList());
    }

    @PostMapping("/{id}/options")
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.OK)
    ProductView addOption(
            @PathVariable UUID id,
            @RequestBody @Valid AddProductOptionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        addProductOptionUseCase.addOption(
                new AddProductOptionUseCase.AddProductOptionCommand(id, storeId, request.name(), request.values()));
        return getProductUseCase.getById(id);
    }

    @PostMapping("/{id}/variants")
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.CREATED)
    VariantView createVariant(
            @PathVariable UUID id,
            @RequestBody @Valid CreateProductVariantRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        return createProductVariantUseCase.createVariant(
                new CreateProductVariantUseCase.CreateProductVariantCommand(
                        id, storeId, request.options(), request.sku(), request.priceOverride()));
    }

    @GetMapping("/{id}/variants")
    List<VariantView> listVariants(@PathVariable UUID id) {
        return getProductUseCase.getById(id).variants();
    }

    @PostMapping("/{id}/variants/{variantId}/stock")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<VariantView> addVariantStock(
            @PathVariable UUID id,
            @PathVariable UUID variantId,
            @Valid @RequestBody AddStockRequest request) {

        return ResponseEntity.ok(addStockUseCase.addStock(new AddStockUseCase.AddStockCommand(variantId, request.quantity())));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<ProductView> addImage(
            @PathVariable UUID id,
            @Valid @RequestBody AddImageRequest request) {

        addProductImageUseCase.addImage(
                new AddProductImageUseCase.AddImageCommand(id, request.url(), request.position(), request.primary()));
        return ResponseEntity.ok(getProductUseCase.getById(id));
    }

    @PutMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<ProductView> updateImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @Valid @RequestBody UpdateImageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        updateProductImageUseCase.updateImage(new UpdateProductImageUseCase.UpdateImageCommand(
                id, storeId, imageId, request.url(), request.position(), request.primary()));
        return ResponseEntity.ok(getProductUseCase.getById(id));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<ProductView> removeImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal UserPrincipal principal) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        removeProductImageUseCase.removeImage(new RemoveProductImageUseCase.RemoveImageCommand(id, storeId, imageId));
        return ResponseEntity.ok(getProductUseCase.getById(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<Void> deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        deactivateProductUseCase.deactivate(new DeactivateProductUseCase.DeactivateCommand(id, storeId));
        return ResponseEntity.noContent().build();
    }
}
