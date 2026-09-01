package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.GetLiveFeedTokenUseCase;
import com.livecomerce.shared.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lives/rtm")
@RequiredArgsConstructor
class LiveFeedTokenController {

    private final GetLiveFeedTokenUseCase getLiveFeedTokenUseCase;

    @GetMapping("/feed-token")
    ResponseEntity<ChatTokenResponse> getFeedToken(@AuthenticationPrincipal UserPrincipal principal) {
        var result = getLiveFeedTokenUseCase.getFeedToken(principal.getUserId());
        return ResponseEntity.ok(new ChatTokenResponse(result.token(), result.channelName(), result.appId()));
    }
}
