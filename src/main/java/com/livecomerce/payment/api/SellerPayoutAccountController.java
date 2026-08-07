package com.livecomerce.payment.api;

import com.livecomerce.payment.application.port.in.CreateConnectOnboardingLinkUseCase;
import com.livecomerce.payment.application.port.in.CreateConnectOnboardingLinkUseCase.CreateOnboardingLinkCommand;
import com.livecomerce.payment.application.port.in.CreateEmbeddedOnboardingSessionUseCase;
import com.livecomerce.payment.application.port.in.GetSellerPayoutAccountDetailsUseCase;
import com.livecomerce.payment.application.port.in.GetSellerPayoutStatusUseCase;
import com.livecomerce.payment.application.port.in.ListSellerPayoutsUseCase;
import com.livecomerce.shared.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller/payout-account")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
class SellerPayoutAccountController {

    private final GetSellerPayoutStatusUseCase getSellerPayoutStatusUseCase;
    private final CreateConnectOnboardingLinkUseCase createConnectOnboardingLinkUseCase;
    private final CreateEmbeddedOnboardingSessionUseCase createEmbeddedOnboardingSessionUseCase;
    private final GetSellerPayoutAccountDetailsUseCase getSellerPayoutAccountDetailsUseCase;
    private final ListSellerPayoutsUseCase listSellerPayoutsUseCase;

    @PostMapping("/onboarding-link")
    ResponseEntity<OnboardingLinkResponse> createOnboardingLink(
            @AuthenticationPrincipal UserPrincipal principal) {

        var result = createConnectOnboardingLinkUseCase.createOnboardingLink(
                new CreateOnboardingLinkCommand(principal.getUserId(), principal.getEmail()));
        return ResponseEntity.ok(new OnboardingLinkResponse(result.url()));
    }

    @PostMapping("/embedded-onboarding-session")
    ResponseEntity<EmbeddedOnboardingSessionResponse> createEmbeddedOnboardingSession(
            @AuthenticationPrincipal UserPrincipal principal) {

        var result = createEmbeddedOnboardingSessionUseCase.createEmbeddedOnboardingSession(
                new CreateEmbeddedOnboardingSessionUseCase.CreateEmbeddedOnboardingSessionCommand(
                        principal.getUserId(), principal.getEmail()));
        return ResponseEntity.ok(new EmbeddedOnboardingSessionResponse(result.clientSecret()));
    }

    @GetMapping("/status")
    ResponseEntity<PayoutStatusResponse> getStatus(
            @AuthenticationPrincipal UserPrincipal principal) {

        var status = getSellerPayoutStatusUseCase.getStatus(principal.getUserId());
        return ResponseEntity.ok(PayoutStatusResponse.from(status));
    }

    @GetMapping("/details")
    ResponseEntity<SellerPayoutAccountDetailsResponse> getDetails(
            @AuthenticationPrincipal UserPrincipal principal) {

        var details = getSellerPayoutAccountDetailsUseCase.getDetails(principal.getUserId());
        return ResponseEntity.ok(SellerPayoutAccountDetailsResponse.from(details));
    }

    @GetMapping("/payouts")
    ResponseEntity<PayoutPageResponse> listPayouts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int limit) {

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        var page = listSellerPayoutsUseCase.listPayouts(principal.getUserId(), cursor, safeLimit);
        return ResponseEntity.ok(PayoutPageResponse.from(page));
    }
}
