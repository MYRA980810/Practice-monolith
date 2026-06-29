package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.GetChatHistoryUseCase;
import com.livecomerce.live.application.port.in.GetChatTokenUseCase;
import com.livecomerce.shared.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lives/{liveId}/rtm")
@RequiredArgsConstructor
class LiveChatController {

    private final GetChatTokenUseCase   getChatTokenUseCase;
    private final GetChatHistoryUseCase getChatHistoryUseCase;

    @GetMapping("/token")
    ResponseEntity<ChatTokenResponse> getChatToken(
            @PathVariable UUID liveId,
            @AuthenticationPrincipal UserPrincipal principal) {

        var result = getChatTokenUseCase.getChatToken(
                new GetChatTokenUseCase.GetChatTokenCommand(liveId, principal.getUserId()));

        return ResponseEntity.ok(new ChatTokenResponse(result.token(), result.channelName(), result.appId()));
    }

    @GetMapping("/history")
    ResponseEntity<List<ChatMessageResponse>> getChatHistory(
            @PathVariable UUID liveId,
            @RequestParam(required = false) OffsetDateTime before,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal UserPrincipal principal) {

        var messages = getChatHistoryUseCase.getChatHistory(
                new GetChatHistoryUseCase.GetChatHistoryCommand(liveId, before, limit));

        return ResponseEntity.ok(messages.stream().map(ChatMessageResponse::from).toList());
    }
}
