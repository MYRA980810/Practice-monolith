package com.livecomerce.live.api;

import com.livecomerce.shared.UserPrincipal;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Real-time feed of active lives over SSE (authenticated like every non-public endpoint; the
 * frontend sends the usual Bearer header). On reconnect the client sends {@code Last-Event-ID}
 * and gets a {@code snapshot} event unless it is already at the current version — see
 * {@link LiveFeedEmitterRegistry#connect}. A user over the per-instance connection cap gets 429.
 */
@RestController
@RequiredArgsConstructor
class LiveFeedStreamController {

    /** Tells nginx-style proxies not to buffer the stream. */
    static final String ACCEL_BUFFERING_HEADER = "X-Accel-Buffering";

    private final LiveFeedEmitterRegistry registry;

    @GetMapping(path = "/api/lives/feed/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<SseEmitter> stream(
            @AuthenticationPrincipal UserPrincipal principal,
            @Nullable @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId) {

        return registry.connect(principal.getUserId(), lastEventId)
                .map(emitter -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noCache().noTransform())
                        .header(ACCEL_BUFFERING_HEADER, "no")
                        .body(emitter))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
    }
}
