package com.livecomerce.notification.api;

import com.livecomerce.notification.application.port.in.MarkNotificationReadUseCase;
import com.livecomerce.notification.application.port.in.MarkNotificationReadUseCase.MarkNotificationReadCommand;
import com.livecomerce.notification.application.port.out.LoadNotificationPort;
import com.livecomerce.shared.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final LoadNotificationPort        loadNotificationPort;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;

    @GetMapping
    public ResponseEntity<?> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<NotificationResponse> result = loadNotificationPort
                .loadByUserId(principal.getUserId(), pageable)
                .map(NotificationResponse::from);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        markNotificationReadUseCase.markRead(new MarkNotificationReadCommand(id, principal.getUserId()));
        return ResponseEntity.ok().build();
    }
}
