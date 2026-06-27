package com.livecomerce.order.api;

import com.livecomerce.order.application.port.in.*;
import com.livecomerce.order.domain.OrderItemType;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
class OrderController {

    private final PlaceOrderItemUseCase placeOrderItemUseCase;
    private final FinalizeOrderUseCase finalizeOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ShipOrderUseCase shipOrderUseCase;

    @PostMapping("/items")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<OrderResponse> placeItem(
            @Valid @RequestBody PlaceOrderItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var order = placeOrderItemUseCase.placeItem(new PlaceOrderItemUseCase.PlaceOrderItemCommand(
                principal.getUserId(),
                request.storeId(),
                request.liveSessionId(),
                request.productId(),
                request.variantId(),
                request.productName(),
                request.unitPrice(),
                request.currency(),
                request.quantity(),
                OrderItemType.PRODUCT
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<OrderResponse> getActive(
            @RequestParam UUID liveSessionId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return getOrderUseCase.getActiveOrder(principal.getUserId(), liveSessionId)
                .map(OrderResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<OrderResponse> finalize(
            @PathVariable UUID id,
            @Valid @RequestBody FinalizeOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var order = finalizeOrderUseCase.finalizeOrder(new FinalizeOrderUseCase.FinalizeOrderCommand(
                id, principal.getUserId(), request.shippingAddress()));
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    ResponseEntity<OrderResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(OrderResponse.from(getOrderUseCase.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<List<OrderResponse>> getByStore(@RequestParam UUID storeId) {
        var orders = getOrderUseCase.getByStore(storeId)
                .stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<OrderResponse> ship(
            @PathVariable UUID id,
            @Valid @RequestBody ShipOrderRequest request) {

        var order = shipOrderUseCase.ship(new ShipOrderUseCase.ShipOrderCommand(id, request.trackingNumber()));
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
