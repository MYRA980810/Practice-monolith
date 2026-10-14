package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.IssueFeedStreamTicketUseCase;
import com.livecomerce.shared.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called server-to-server by the Next.js app (with the Bearer token) right before the browser opens
 * {@code GET /api/lives/feed/stream} with {@code X-Stream-Ticket}. BUYER-only like the rest of the
 * lives feed. The ticket is a credential: never cached.
 */
@RestController
@RequiredArgsConstructor
class LiveFeedTicketController {

    private final IssueFeedStreamTicketUseCase issueFeedStreamTicketUseCase;

    @PreAuthorize("hasRole('BUYER')")
    @PostMapping("/api/lives/feed/stream-ticket")
    ResponseEntity<StreamTicketResponse> issue(@AuthenticationPrincipal UserPrincipal principal) {
        var ticket = issueFeedStreamTicketUseCase.issue(principal.getUserId());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new StreamTicketResponse(ticket.ticket(), ticket.expiresInMs()));
    }
}
