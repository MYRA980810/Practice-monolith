package com.livecomerce.billing.api;

import com.livecomerce.billing.application.port.in.CreateCheckoutUseCase;
import com.livecomerce.billing.application.port.in.GetSubscriptionUseCase;
import com.livecomerce.billing.application.port.in.HandleWebhookUseCase;
import com.livecomerce.billing.domain.Gateway;
import com.livecomerce.shared.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
class BillingController {

    private final CreateCheckoutUseCase createCheckoutUseCase;
    private final HandleWebhookUseCase handleWebhookUseCase;
    private final GetSubscriptionUseCase getSubscriptionUseCase;

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<CheckoutResponse> createCheckout(
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var result = createCheckoutUseCase.createCheckout(
                new CreateCheckoutUseCase.CreateCheckoutCommand(
                        principal.getUserId(),
                        request.plan(),
                        request.gateway()
                ));
        return ResponseEntity.ok(new CheckoutResponse(result.checkoutUrl()));
    }

    @PostMapping("/webhook/{gateway}")
    ResponseEntity<Void> handleWebhook(
            @PathVariable Gateway gateway,
            @RequestBody String payload,
            @RequestHeader(value = "X-Signature", required = false) String signature) {

        handleWebhookUseCase.handle(
                new HandleWebhookUseCase.WebhookCommand(gateway, payload, signature));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/subscription/me")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<SubscriptionResponse> getMySubscription(
            @AuthenticationPrincipal UserPrincipal principal) {

        var subscription = getSubscriptionUseCase.getByUserId(principal.getUserId());
        return ResponseEntity.ok(SubscriptionResponse.from(subscription));
    }
}
