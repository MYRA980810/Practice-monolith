package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.BuyLiveProductUseCase;
import com.livecomerce.live.application.port.in.ExitLiveUseCase;
import com.livecomerce.order.api.OrderResponse;
import com.livecomerce.shared.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/lives/{liveId}")
@RequiredArgsConstructor
class LiveBuyController {

    private final BuyLiveProductUseCase buyLiveProductUseCase;
    private final ExitLiveUseCase exitLiveUseCase;

    @PostMapping("/buy")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<OrderResponse> buyProduct(
            @PathVariable UUID liveId,
            @Valid @RequestBody BuyLiveProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var order = buyLiveProductUseCase.buyLiveProduct(new BuyLiveProductUseCase.BuyLiveProductCommand(
                liveId, request.liveProductId(), principal.getUserId(), request.quantity()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @PostMapping("/exit")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<ExitLiveApiResponse> exitLive(
            @PathVariable UUID liveId,
            @RequestBody ExitLiveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var result = exitLiveUseCase.exitLive(new ExitLiveUseCase.ExitLiveCommand(
                liveId, principal.getUserId(), request.shippingAddress(), request.stripePaymentMethodId()
        ));

        if (result.order() == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(ExitLiveApiResponse.from(result));
    }
}
