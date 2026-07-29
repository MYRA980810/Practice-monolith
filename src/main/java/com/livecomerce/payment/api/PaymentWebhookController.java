package com.livecomerce.payment.api;

import com.livecomerce.payment.application.port.in.HandleStripeWebhookUseCase;
import com.livecomerce.payment.application.port.in.HandleStripeWebhookUseCase.WebhookCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Public endpoint, invoked by Stripe itself — no @PreAuthorize. Signature verification
// (StripeWebhookGatewayPort.verifyAndParse, via Stripe-Signature) is what stands in for auth here.
@RestController
@RequestMapping("/api/payment/webhook")
@RequiredArgsConstructor
class PaymentWebhookController {

    private final HandleStripeWebhookUseCase handleStripeWebhookUseCase;

    @PostMapping
    ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        handleStripeWebhookUseCase.handle(new WebhookCommand(payload, signature));
        return ResponseEntity.ok().build();
    }
}
