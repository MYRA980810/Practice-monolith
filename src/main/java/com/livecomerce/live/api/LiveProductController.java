package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.AddCatalogProductUseCase;
import com.livecomerce.live.application.port.in.AddHotProductUseCase;
import com.livecomerce.live.application.port.in.ExpirePinUseCase;
import com.livecomerce.live.application.port.in.PinProductUseCase;
import com.livecomerce.live.application.port.in.ReorderProductsUseCase;
import com.livecomerce.live.application.port.in.UnpinProductUseCase;
import com.livecomerce.live.application.port.out.LoadLiveProductPort;
import com.livecomerce.shared.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lives/{liveId}/products")
@RequiredArgsConstructor
class LiveProductController {

    private final AddCatalogProductUseCase addCatalogProductUseCase;
    private final AddHotProductUseCase addHotProductUseCase;
    private final PinProductUseCase pinProductUseCase;
    private final UnpinProductUseCase unpinProductUseCase;
    private final ExpirePinUseCase expirePinUseCase;
    private final ReorderProductsUseCase reorderProductsUseCase;
    private final LoadLiveProductPort loadLiveProductPort;

    @PostMapping("/catalog")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<LiveProductResponse> addCatalogProduct(
            @PathVariable UUID liveId,
            @Valid @RequestBody AddCatalogProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var product = addCatalogProductUseCase.addCatalogProduct(new AddCatalogProductUseCase.AddCatalogProductCommand(
                liveId,
                principal.getUserId(),
                request.productId(),
                request.variantId(),
                request.nameSnapshot(),
                request.priceSnapshot(),
                request.currency(),
                request.stockAllocated()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(LiveProductResponse.from(product));
    }

    @PostMapping("/hot")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<LiveProductResponse> addHotProduct(
            @PathVariable UUID liveId,
            @Valid @RequestBody AddHotProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var product = addHotProductUseCase.addHotProduct(new AddHotProductUseCase.AddHotProductCommand(
                liveId,
                principal.getUserId(),
                request.name(),
                request.price(),
                request.currency(),
                request.stockAllocated(),
                request.imageUrl()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(LiveProductResponse.from(product));
    }

    @PostMapping("/{id}/pin")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<LiveProductResponse> pinProduct(
            @PathVariable UUID liveId,
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        var product = pinProductUseCase.pinProduct(
                new PinProductUseCase.PinProductCommand(liveId, id, principal.getUserId())
        );
        return ResponseEntity.ok(LiveProductResponse.from(product));
    }

    @PostMapping("/{id}/expire-pin")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<LiveProductResponse> expirePin(
            @PathVariable UUID liveId,
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        var product = expirePinUseCase.expirePin(
                new ExpirePinUseCase.ExpirePinCommand(liveId, id, principal.getUserId())
        );
        return ResponseEntity.ok(LiveProductResponse.from(product));
    }

    @DeleteMapping("/{id}/pin")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<LiveProductResponse> unpinProduct(
            @PathVariable UUID liveId,
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        var product = unpinProductUseCase.unpinProduct(
                new UnpinProductUseCase.UnpinProductCommand(liveId, id, principal.getUserId())
        );
        return ResponseEntity.ok(LiveProductResponse.from(product));
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<List<LiveProductResponse>> reorderProducts(
            @PathVariable UUID liveId,
            @Valid @RequestBody ReorderProductsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var products = reorderProductsUseCase.reorderProducts(new ReorderProductsUseCase.ReorderProductsCommand(
                liveId, principal.getUserId(), request.orderedProductIds()
        ));
        return ResponseEntity.ok(products.stream().map(LiveProductResponse::from).toList());
    }

    @GetMapping
    ResponseEntity<List<LiveProductResponse>> listProducts(@PathVariable UUID liveId) {
        var products = loadLiveProductPort.loadByLiveId(liveId);
        return ResponseEntity.ok(products.stream().map(LiveProductResponse::from).toList());
    }
}
