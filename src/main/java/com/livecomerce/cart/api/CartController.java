package com.livecomerce.cart.api;

import com.livecomerce.cart.application.port.in.AddToCartUseCase;
import com.livecomerce.cart.application.port.in.ChangeQuantityUseCase;
import com.livecomerce.cart.application.port.in.CheckoutCartUseCase;
import com.livecomerce.cart.application.port.in.GetCombinedCartViewUseCase;
import com.livecomerce.cart.application.port.in.RemoveFromCartUseCase;
import com.livecomerce.shared.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Buyer-facing storefront cart endpoints. Add/change-quantity rejections are
 * modeled as {@code Result} records at the application layer (design D1/D8),
 * not exceptions, so this controller maps each machine-readable reason code
 * to an HTTP status directly rather than relying on {@code
 * CartExceptionHandler}. Checkout always answers HTTP 200 (design D8) — the
 * response body is the report, including partial/total failure.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
class CartController {

    private final AddToCartUseCase addToCartUseCase;
    private final ChangeQuantityUseCase changeQuantityUseCase;
    private final RemoveFromCartUseCase removeFromCartUseCase;
    private final GetCombinedCartViewUseCase getCombinedCartViewUseCase;
    private final CheckoutCartUseCase checkoutCartUseCase;

    @PostMapping("/stores/{storeId}/items")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<AddToCartResponse> addToCart(
            @PathVariable UUID storeId,
            @Valid @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var result = addToCartUseCase.addToCart(new AddToCartUseCase.AddToCartCommand(
                principal.getUserId(), storeId, request.productId(), request.variantId(), request.quantity()));

        var body = AddToCartResponse.from(result);
        if (result.success()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }
        return ResponseEntity.status(statusForRejection(result.rejectionReason())).body(body);
    }

    @PostMapping("/stores/{storeId}/items/{productId}/increment")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<ChangeQuantityResponse> increment(
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @RequestParam(required = false) UUID variantId,
            @RequestParam(defaultValue = "1") int delta,
            @AuthenticationPrincipal UserPrincipal principal) {

        return applyDelta(storeId, productId, variantId, delta, principal);
    }

    @PostMapping("/stores/{storeId}/items/{productId}/decrement")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<ChangeQuantityResponse> decrement(
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @RequestParam(required = false) UUID variantId,
            @RequestParam(defaultValue = "1") int delta,
            @AuthenticationPrincipal UserPrincipal principal) {

        return applyDelta(storeId, productId, variantId, -delta, principal);
    }

    private ResponseEntity<ChangeQuantityResponse> applyDelta(
            UUID storeId, UUID productId, UUID variantId, int delta, UserPrincipal principal) {

        var result = changeQuantityUseCase.changeQuantity(new ChangeQuantityUseCase.ChangeQuantityCommand(
                principal.getUserId(), storeId, productId, variantId, delta));

        var body = ChangeQuantityResponse.from(result);
        if (result.success()) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.status(statusForFailure(result.failureReason())).body(body);
    }

    @DeleteMapping("/stores/{storeId}/items/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<Void> removeLine(
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @RequestParam(required = false) UUID variantId,
            @AuthenticationPrincipal UserPrincipal principal) {

        removeFromCartUseCase.removeLine(new RemoveFromCartUseCase.RemoveFromCartCommand(
                principal.getUserId(), storeId, productId, variantId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<CombinedCartResponse> getCombinedView(@AuthenticationPrincipal UserPrincipal principal) {
        var view = getCombinedCartViewUseCase.getCombinedView(principal.getUserId());
        return ResponseEntity.ok(CombinedCartResponse.from(view));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<CheckoutCartResponse> checkout(
            @Valid @RequestBody CheckoutCartRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var selectedItems = request.selectedItems().stream()
                .map(item -> new CheckoutCartUseCase.SelectedItem(item.storeId(), item.productId(), item.variantId()))
                .toList();
        var response = checkoutCartUseCase.checkout(
                new CheckoutCartUseCase.CheckoutCartCommand(principal.getUserId(), selectedItems));
        // Always HTTP 200 (design D8) — the envelope is the report, never a transport error.
        return ResponseEntity.ok(CheckoutCartResponse.from(response));
    }

    /** {@code QUANTITY_LIMIT_EXCEEDED} is a request-side bound violation; everything else is a state conflict. */
    private static HttpStatus statusForRejection(String reason) {
        return "QUANTITY_LIMIT_EXCEEDED".equals(reason) ? HttpStatus.BAD_REQUEST : HttpStatus.CONFLICT;
    }

    /** {@code LINE_NOT_FOUND} maps to 404; {@code QUANTITY_LIMIT_EXCEEDED} to 400; the rest are state conflicts. */
    private static HttpStatus statusForFailure(String reason) {
        return switch (reason) {
            case "LINE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "QUANTITY_LIMIT_EXCEEDED" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.CONFLICT;
        };
    }
}
