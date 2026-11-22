package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase;
import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase.BanChatUserCommand;
import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase.DeleteChatMessageCommand;
import com.livecomerce.live.application.port.in.ModerateLiveChatUseCase.UnbanChatUserCommand;
import com.livecomerce.shared.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Chat moderation of a live by its seller (bans are per seller; messages are IVS Chat messages). */
@RestController
@RequestMapping("/api/lives/{liveId}/chat")
@RequiredArgsConstructor
class IvsChatModerationController {

    /**
     * Upper bound for the IVS message id path variable, rejected with 400 before calling IVS. The SDK
     * model carries no length constraint, so this is a defensive bound, not the exact IVS id format.
     */
    static final int MAX_MESSAGE_ID_LENGTH = 128;

    private final ModerateLiveChatUseCase moderateLiveChatUseCase;

    /** Idempotent: banning an already banned user answers 204 and retries the disconnect. */
    @PostMapping("/bans")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<Void> banUser(
            @PathVariable UUID liveId,
            @Valid @RequestBody BanChatUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        moderateLiveChatUseCase.banUser(
                new BanChatUserCommand(liveId, principal.getUserId(), request.userId(), request.reason()));
        return ResponseEntity.noContent().build();
    }

    /** Idempotent: 204 even if the user was not banned. */
    @DeleteMapping("/bans/{userId}")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<Void> unbanUser(
            @PathVariable UUID liveId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal) {

        moderateLiveChatUseCase.unbanUser(new UnbanChatUserCommand(liveId, principal.getUserId(), userId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/messages/{messageId}")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<Void> deleteMessage(
            @PathVariable UUID liveId,
            @PathVariable @NotBlank @Size(max = MAX_MESSAGE_ID_LENGTH) String messageId,
            @AuthenticationPrincipal UserPrincipal principal) {

        moderateLiveChatUseCase.deleteMessage(new DeleteChatMessageCommand(liveId, principal.getUserId(), messageId));
        return ResponseEntity.noContent().build();
    }
}
