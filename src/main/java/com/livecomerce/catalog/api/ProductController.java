package com.livecomerce.catalog.api;

import com.livecomerce.catalog.application.port.in.AddProductImageUseCase;
import com.livecomerce.catalog.application.port.in.AddStockUseCase;
import com.livecomerce.catalog.application.port.in.CreateProductUseCase;
import com.livecomerce.catalog.application.port.in.DeactivateProductUseCase;
import com.livecomerce.catalog.application.port.in.GetProductUseCase;
import com.livecomerce.catalog.application.port.in.RemoveProductImageUseCase;
import com.livecomerce.catalog.application.port.in.UpdateProductImageUseCase;
import com.livecomerce.catalog.application.port.in.UpdateProductUseCase;
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
    private final AddStockUseCase addStockUseCase;
    private final AddProductImageUseCase addProductImageUseCase;
    private final UpdateProductImageUseCase updateProductImageUseCase;
    private final RemoveProductImageUseCase removeProductImageUseCase;
    private final DeactivateProductUseCase deactivateProductUseCase;
    private final GetStoreUseCase getStoreUseCase;

    private static final String DEFAULT_CURRENCY = "MXN";

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        var product = createProductUseCase.create(new CreateProductUseCase.CreateProductCommand(
                storeId,
                request.name(),
                request.description(),
                request.basePrice(),
                Objects.requireNonNullElse(request.currency(), DEFAULT_CURRENCY),
                request.sku()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<ProductResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        var product = updateProductUseCase.update(new UpdateProductUseCase.UpdateProductCommand(
                id,
                storeId,
                request.name(),
                request.description(),
                request.basePrice(),
                Objects.requireNonNullElse(request.currency(), DEFAULT_CURRENCY),
                request.sku()
        ));
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @GetMapping("/{id}")
    ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        var product = getProductUseCase.getById(id);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @GetMapping
    ResponseEntity<List<ProductResponse>> getByStore(@RequestParam UUID storeId) {
        var products = getProductUseCase.getByStoreId(storeId).stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<List<ProductResponse>> getMyProducts(@AuthenticationPrincipal UserPrincipal principal) {
        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        var products = getProductUseCase.getByStoreId(storeId).stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(products);
    }

    @PostMapping("/{id}/stock")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<ProductResponse> addStock(
            @PathVariable UUID id,
            @Valid @RequestBody AddStockRequest request) {

        var product = addStockUseCase.addStock(
                new AddStockUseCase.AddStockCommand(id, request.quantity()));
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<ProductResponse> addImage(
            @PathVariable UUID id,
            @Valid @RequestBody AddImageRequest request) {

        var product = addProductImageUseCase.addImage(
                new AddProductImageUseCase.AddImageCommand(id, request.url(), request.position(), request.primary()));
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @PutMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<ProductResponse> updateImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @Valid @RequestBody UpdateImageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        var product = updateProductImageUseCase.updateImage(new UpdateProductImageUseCase.UpdateImageCommand(
                id, storeId, imageId, request.url(), request.position(), request.primary()));
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<ProductResponse> removeImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal UserPrincipal principal) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        var product = removeProductImageUseCase.removeImage(new RemoveProductImageUseCase.RemoveImageCommand(
                id, storeId, imageId));
        return ResponseEntity.ok(ProductResponse.from(product));
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
