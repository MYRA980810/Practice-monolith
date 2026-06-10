package com.livecomerce.store.api;

import com.livecomerce.shared.UserPrincipal;
import com.livecomerce.store.application.port.in.ChangePlanUseCase;
import com.livecomerce.store.application.port.in.CreateStoreUseCase;
import com.livecomerce.store.application.port.in.DeactivateStoreUseCase;
import com.livecomerce.store.application.port.in.GetStoreUseCase;
import com.livecomerce.store.application.port.in.ListStoresUseCase;
import com.livecomerce.store.application.port.in.ReactivateStoreUseCase;
import com.livecomerce.store.application.port.in.UpdateStoreUseCase;
import com.livecomerce.shared.Plan;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
class StoreController {

    private final CreateStoreUseCase createStoreUseCase;
    private final GetStoreUseCase getStoreUseCase;
    private final UpdateStoreUseCase updateStoreUseCase;
    private final ChangePlanUseCase changePlanUseCase;
    private final DeactivateStoreUseCase deactivateStoreUseCase;
    private final ReactivateStoreUseCase reactivateStoreUseCase;
    private final ListStoresUseCase listStoresUseCase;

    @GetMapping
    ResponseEntity<Page<StoreCardResponse>> listStores(
            @PageableDefault(size = 20) Pageable pageable) {
        var page = listStoresUseCase.listActive(pageable).map(StoreCardResponse::from);
        return ResponseEntity.ok(page);
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<StoreResponse> create(
            @Valid @RequestBody CreateStoreRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var store = createStoreUseCase.create(new CreateStoreUseCase.CreateStoreCommand(
                principal.getUserId(),
                request.name(),
                request.slug(),
                request.description(),
                request.logoUrl()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(StoreResponse.from(store));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<StoreResponse> getMyStore(@AuthenticationPrincipal UserPrincipal principal) {
        var store = getStoreUseCase.getByUserId(principal.getUserId());
        return ResponseEntity.ok(StoreResponse.from(store));
    }

    @GetMapping("/{slug}")
    ResponseEntity<StoreCardResponse> getBySlug(@PathVariable String slug) {
        var store = getStoreUseCase.getBySlug(slug);
        return ResponseEntity.ok(StoreCardResponse.from(store));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<StoreResponse> updateMyStore(
            @Valid @RequestBody UpdateStoreRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var store = updateStoreUseCase.update(new UpdateStoreUseCase.UpdateStoreCommand(
                principal.getUserId(),
                request.name(),
                request.description(),
                request.logoUrl()
        ));
        return ResponseEntity.ok(StoreResponse.from(store));
    }

    @GetMapping("/plans")
    ResponseEntity<List<PlanResponse>> getPlans() {
        var plans = Arrays.stream(Plan.values())
                .map(PlanResponse::from)
                .toList();
        return ResponseEntity.ok(plans);
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<Void> deactivateMyStore(@AuthenticationPrincipal UserPrincipal principal) {
        deactivateStoreUseCase.deactivate(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/reactivate")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<StoreResponse> reactivateMyStore(@AuthenticationPrincipal UserPrincipal principal) {
        reactivateStoreUseCase.reactivate(principal.getUserId());
        var store = getStoreUseCase.getByUserId(principal.getUserId());
        return ResponseEntity.ok(StoreResponse.from(store));
    }

    @PutMapping("/me/plan")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<StoreResponse> changeMyPlan(
            @Valid @RequestBody ChangePlanRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var store = changePlanUseCase.change(new ChangePlanUseCase.ChangePlanCommand(
                principal.getUserId(),
                request.plan()
        ));
        return ResponseEntity.ok(StoreResponse.from(store));
    }
}
