package com.livecomerce.catalog.api;

import com.livecomerce.catalog.application.port.in.AddProductImageUseCase;
import com.livecomerce.catalog.application.port.in.AddStockUseCase;
import com.livecomerce.catalog.application.port.in.CreateProductUseCase;
import com.livecomerce.catalog.application.port.in.GetProductUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final GetProductUseCase getProductUseCase;
    private final AddStockUseCase addStockUseCase;
    private final AddProductImageUseCase addProductImageUseCase;

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest request) {

        var product = createProductUseCase.create(new CreateProductUseCase.CreateProductCommand(
                request.storeId(),
                request.name(),
                request.description(),
                request.basePrice(),
                request.currency(),
                request.sku()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
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
}
