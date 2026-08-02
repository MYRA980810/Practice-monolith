package com.livecomerce.payment.api;

import com.livecomerce.payment.application.port.in.CreateSetupIntentUseCase;
import com.livecomerce.payment.application.port.in.CreateSetupIntentUseCase.CreateSetupIntentCommand;
import com.livecomerce.payment.application.port.in.DeleteBuyerPaymentMethodUseCase;
import com.livecomerce.payment.application.port.in.GetBuyerPaymentMethodsUseCase;
import com.livecomerce.payment.application.port.in.SetDefaultBuyerPaymentMethodUseCase;
import com.livecomerce.shared.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// NOTE: no POST / here on purpose. A BuyerPaymentMethod row is only ever created from the
// Stripe webhook (increment 5), never from a client payload, so the client can never invent
// its own brand/last4 or "confirm" a save without actually completing 3DS.
// POST /setup-intent only kicks off the Stripe SetupIntent; the row itself lands via webhook.
@RestController
@RequestMapping("/api/buyer/payment-methods")
@PreAuthorize("hasRole('BUYER')")
@RequiredArgsConstructor
class BuyerPaymentMethodController {

    private final GetBuyerPaymentMethodsUseCase getBuyerPaymentMethodsUseCase;
    private final SetDefaultBuyerPaymentMethodUseCase setDefaultBuyerPaymentMethodUseCase;
    private final DeleteBuyerPaymentMethodUseCase deleteBuyerPaymentMethodUseCase;
    private final CreateSetupIntentUseCase createSetupIntentUseCase;

    @PostMapping("/setup-intent")
    ResponseEntity<SetupIntentResponse> createSetupIntent(
            @AuthenticationPrincipal UserPrincipal principal) {

        var result = createSetupIntentUseCase.createSetupIntent(
                new CreateSetupIntentCommand(principal.getUserId(), principal.getEmail()));
        return ResponseEntity.ok(new SetupIntentResponse(result.clientSecret()));
    }

    @GetMapping
    ResponseEntity<List<BuyerPaymentMethodResponse>> getPaymentMethods(
            @AuthenticationPrincipal UserPrincipal principal) {

        var paymentMethods = getBuyerPaymentMethodsUseCase.listByUserId(principal.getUserId())
                .stream()
                .map(BuyerPaymentMethodResponse::from)
                .toList();
        return ResponseEntity.ok(paymentMethods);
    }

    @PatchMapping("/{paymentMethodId}/default")
    ResponseEntity<Void> setDefault(
            @PathVariable UUID paymentMethodId,
            @AuthenticationPrincipal UserPrincipal principal) {

        setDefaultBuyerPaymentMethodUseCase.setDefault(principal.getUserId(), paymentMethodId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{paymentMethodId}")
    ResponseEntity<Void> deletePaymentMethod(
            @PathVariable UUID paymentMethodId,
            @AuthenticationPrincipal UserPrincipal principal) {

        deleteBuyerPaymentMethodUseCase.delete(principal.getUserId(), paymentMethodId);
        return ResponseEntity.noContent().build();
    }
}
