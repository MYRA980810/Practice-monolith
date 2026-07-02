package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.CheckLiveSubscriptionUseCase;
import com.livecomerce.live.application.port.in.CheckLiveSubscriptionUseCase.CheckLiveSubscriptionCommand;
import com.livecomerce.live.application.port.in.SubscribeToLiveUseCase;
import com.livecomerce.live.application.port.in.SubscribeToLiveUseCase.SubscribeToLiveCommand;
import com.livecomerce.live.application.port.in.UnsubscribeFromLiveUseCase;
import com.livecomerce.live.application.port.in.UnsubscribeFromLiveUseCase.UnsubscribeFromLiveCommand;
import com.livecomerce.live.domain.LiveSubscriptionNotForSellerException;
import com.livecomerce.shared.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
class LiveSubscriptionController {

    private final SubscribeToLiveUseCase      subscribeToLiveUseCase;
    private final UnsubscribeFromLiveUseCase  unsubscribeFromLiveUseCase;
    private final CheckLiveSubscriptionUseCase checkLiveSubscriptionUseCase;

    @PostMapping("/api/lives/{liveId}/subscribe")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<Void> subscribe(
            @PathVariable UUID liveId,
            @AuthenticationPrincipal UserPrincipal principal) {

        requireBuyer(principal);
        subscribeToLiveUseCase.subscribe(new SubscribeToLiveCommand(liveId, principal.getUserId()));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/lives/{liveId}/subscribe")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<Void> unsubscribe(
            @PathVariable UUID liveId,
            @AuthenticationPrincipal UserPrincipal principal) {

        requireBuyer(principal);
        unsubscribeFromLiveUseCase.unsubscribe(new UnsubscribeFromLiveCommand(liveId, principal.getUserId()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/lives/{liveId}/subscribed")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<SubscribedResponse> isSubscribed(
            @PathVariable UUID liveId,
            @AuthenticationPrincipal UserPrincipal principal) {

        requireBuyer(principal);
        boolean subscribed = checkLiveSubscriptionUseCase.isSubscribed(
                new CheckLiveSubscriptionCommand(liveId, principal.getUserId()));
        return ResponseEntity.ok(new SubscribedResponse(subscribed));
    }

    /**
     * Defense-in-depth role guard. The primary guard is {@code @PreAuthorize}; this
     * programmatic check ensures the rule is enforceable in unit tests that exclude
     * Spring Security auto-configuration.
     */
    private void requireBuyer(UserPrincipal principal) {
        boolean isBuyer = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_BUYER"));
        if (!isBuyer) {
            throw new LiveSubscriptionNotForSellerException();
        }
    }

    record SubscribedResponse(boolean subscribed) {}
}
