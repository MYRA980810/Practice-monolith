package com.livecomerce.payment.api;

import com.livecomerce.payment.application.port.in.CreateConnectOnboardingLinkUseCase;
import com.livecomerce.payment.application.port.in.CreateConnectOnboardingLinkUseCase.CreateOnboardingLinkCommand;
import com.livecomerce.payment.application.port.in.GetSellerPayoutStatusUseCase;
import com.livecomerce.shared.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller/payout-account")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
class SellerPayoutAccountController {

    private final GetSellerPayoutStatusUseCase getSellerPayoutStatusUseCase;
    private final CreateConnectOnboardingLinkUseCase createConnectOnboardingLinkUseCase;

    @PostMapping("/onboarding-link")
    ResponseEntity<OnboardingLinkResponse> createOnboardingLink(
            @AuthenticationPrincipal UserPrincipal principal) {

        var result = createConnectOnboardingLinkUseCase.createOnboardingLink(
                new CreateOnboardingLinkCommand(principal.getUserId(), principal.getContact()));
        return ResponseEntity.ok(new OnboardingLinkResponse(result.url()));
    }

    @GetMapping("/status")
    ResponseEntity<PayoutStatusResponse> getStatus(
            @AuthenticationPrincipal UserPrincipal principal) {

        var status = getSellerPayoutStatusUseCase.getStatus(principal.getUserId());
        return ResponseEntity.ok(PayoutStatusResponse.from(status));
    }
}
