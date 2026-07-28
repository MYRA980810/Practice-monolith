package com.livecomerce.live.api;

import com.livecomerce.live.application.port.in.CancelLiveUseCase;
import com.livecomerce.live.application.port.in.CreateLiveUseCase;
import com.livecomerce.live.application.port.in.EndLiveUseCase;
import com.livecomerce.live.application.port.in.RecordViewerHeartbeatUseCase;
import com.livecomerce.live.application.port.in.StartLiveUseCase;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveStatus;
import com.livecomerce.shared.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
class LiveController {

    private final CreateLiveUseCase createLiveUseCase;
    private final StartLiveUseCase startLiveUseCase;
    private final EndLiveUseCase endLiveUseCase;
    private final CancelLiveUseCase cancelLiveUseCase;
    private final RecordViewerHeartbeatUseCase recordViewerHeartbeatUseCase;
    private final LoadLivePort loadLivePort;

    @PostMapping("/api/lives")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<LiveResponse> createLive(
            @Valid @RequestBody CreateLiveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var live = createLiveUseCase.createLive(new CreateLiveUseCase.CreateLiveCommand(
                principal.getUserId(),
                request.storeId(),
                request.context(),
                request.title(),
                request.thumbnailUrl(),
                request.scheduledAt(),
                Objects.requireNonNullElse(request.displayDurationSeconds(), 60)
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(LiveResponse.from(live));
    }

    @PostMapping("/api/lives/{id}/start")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<LiveResponse> startLive(
            @PathVariable UUID id,
            @Valid @RequestBody StartLiveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var live = startLiveUseCase.startLive(new StartLiveUseCase.StartLiveCommand(
                id, principal.getUserId(), request.rtcUid()
        ));
        return ResponseEntity.ok(LiveResponse.from(live));
    }

    @PostMapping("/api/lives/{id}/end")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<LiveResponse> endLive(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        var live = endLiveUseCase.endLive(new EndLiveUseCase.EndLiveCommand(id, principal.getUserId()));
        return ResponseEntity.ok(LiveResponse.from(live));
    }

    @PostMapping("/api/lives/{id}/cancel")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<LiveResponse> cancelLive(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        var live = cancelLiveUseCase.cancelLive(new CancelLiveUseCase.CancelLiveCommand(id, principal.getUserId()));
        return ResponseEntity.ok(LiveResponse.from(live));
    }

    @PostMapping("/api/lives/{id}/heartbeat")
    ResponseEntity<HeartbeatResponse> heartbeat(
            @PathVariable UUID id,
            @Valid @RequestBody HeartbeatRequest request) {

        long count = recordViewerHeartbeatUseCase.recordHeartbeat(
                new RecordViewerHeartbeatUseCase.RecordHeartbeatCommand(id, request.viewerId()));
        return ResponseEntity.ok(new HeartbeatResponse(count));
    }

    @GetMapping("/api/lives/{id}")
    ResponseEntity<LiveResponse> getLive(@PathVariable UUID id) {
        return loadLivePort.loadById(id)
                .map(LiveResponse::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new LiveNotFoundException(id));
    }

    @GetMapping("/api/lives")
    ResponseEntity<List<LiveResponse>> listBySeller(
            @RequestParam UUID sellerId,
            @RequestParam(required = false) String status) {

        var lives = status != null
                ? loadLivePort.loadBySellerIdAndStatus(sellerId, LiveStatus.fromCode(status))
                : loadLivePort.loadBySellerId(sellerId);

        return ResponseEntity.ok(lives.stream().map(LiveResponse::from).toList());
    }

    @GetMapping("/api/stores/{storeId}/lives")
    ResponseEntity<List<LiveResponse>> listByStore(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String status) {

        var lives = status != null
                ? loadLivePort.loadByStoreIdAndStatus(storeId, LiveStatus.fromCode(status))
                : loadLivePort.loadByStoreId(storeId);

        return ResponseEntity.ok(lives.stream().map(LiveResponse::from).toList());
    }
}
