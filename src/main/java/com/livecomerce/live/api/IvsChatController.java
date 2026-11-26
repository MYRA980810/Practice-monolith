package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.GetIvsChatTokenUseCase;
import com.livecomerce.live.application.port.in.GetIvsChatTokenUseCase.GetIvsChatTokenCommand;
import com.livecomerce.shared.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Amazon IVS Chat endpoints of a live (used when live.chat-provider=ivs). */
@RestController
@RequestMapping("/api/lives/{liveId}/chat")
@RequiredArgsConstructor
class IvsChatController {

    private final GetIvsChatTokenUseCase getIvsChatTokenUseCase;

    @GetMapping("/token")
    ResponseEntity<IvsChatTokenResponse> getChatToken(
            @PathVariable UUID liveId,
            @AuthenticationPrincipal UserPrincipal principal) {

        var result = getIvsChatTokenUseCase.getIvsChatToken(
                new GetIvsChatTokenCommand(liveId, principal.getUserId()));

        return ResponseEntity.ok(new IvsChatTokenResponse(
                result.token(), result.sessionExpirationTime(), result.tokenExpirationTime(), result.region(), result.canSend()));
    }
}
