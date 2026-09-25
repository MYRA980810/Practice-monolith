package com.livecomerce.store.api;

import com.livecomerce.shared.UserPrincipal;
import com.livecomerce.store.LoadStoreRatingPort;
import com.livecomerce.store.StoreCategoryPort;
import com.livecomerce.store.application.port.in.ChangePlanUseCase;
import com.livecomerce.store.application.port.in.CloseStoreTemporarilyUseCase;
import com.livecomerce.store.application.port.in.CreateStoreUseCase;
import com.livecomerce.store.application.port.in.DeactivateStoreUseCase;
import com.livecomerce.store.application.port.in.FollowStoreUseCase;
import com.livecomerce.store.application.port.in.GetStoreFollowersUseCase;
import com.livecomerce.store.application.port.in.GetStoreUseCase;
import com.livecomerce.store.application.port.in.ListStoresUseCase;
import com.livecomerce.store.application.port.in.ReactivateStoreUseCase;
import com.livecomerce.store.application.port.in.ReopenStoreUseCase;
import com.livecomerce.store.application.port.in.SetStoreCategoryUseCase;
import com.livecomerce.store.application.port.in.UnfollowStoreUseCase;
import com.livecomerce.store.application.port.in.UpdateStoreUseCase;
import com.livecomerce.store.application.port.out.LoadStoreLiveStatusPort;
import com.livecomerce.store.application.port.out.LoadStoreRankPort;
import com.livecomerce.store.domain.AddressType;
import com.livecomerce.store.domain.Store;
import com.livecomerce.shared.Plan;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// Public: referenced from com.livecomerce.auth.infrastructure.security.StoreFollowerPermitAllTest
// to verify the followers/count and following routes bypass the global filter's 401
// for anonymous requests (same reason AgoraSignalingWebhookController is public).
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private static final Logger log = LoggerFactory.getLogger(StoreController.class);

    private final CreateStoreUseCase createStoreUseCase;
    private final GetStoreUseCase getStoreUseCase;
    private final UpdateStoreUseCase updateStoreUseCase;
    private final ChangePlanUseCase changePlanUseCase;
    private final DeactivateStoreUseCase deactivateStoreUseCase;
    private final ReactivateStoreUseCase reactivateStoreUseCase;
    private final CloseStoreTemporarilyUseCase closeStoreTemporarilyUseCase;
    private final ReopenStoreUseCase reopenStoreUseCase;
    private final ListStoresUseCase listStoresUseCase;
    private final FollowStoreUseCase followStoreUseCase;
    private final UnfollowStoreUseCase unfollowStoreUseCase;
    private final GetStoreFollowersUseCase getStoreFollowersUseCase;
    private final LoadStoreRatingPort loadStoreRatingPort;
    private final LoadStoreRankPort loadStoreRankPort;
    private final LoadStoreLiveStatusPort loadStoreLiveStatusPort;
    private final StoreCategoryPort storeCategoryPort;
    private final SetStoreCategoryUseCase setStoreCategoryUseCase;

    @GetMapping
    ResponseEntity<Page<StoreCardResponse>> listStores(
            @PageableDefault(size = 20) Pageable pageable) {
        var page = listStoresUseCase.listActive(pageable);
        var storeIds = page.getContent().stream().map(Store::getId).collect(Collectors.toSet());
        var ratings = loadRatingsSafely(storeIds);
        var ranks = loadRanksSafely(storeIds);
        var followerCounts = loadFollowerCountsSafely(storeIds);
        var activeLiveIds = loadLiveNowSafely(storeIds);
        var categories = loadCategoriesSafely(page.getContent());
        return ResponseEntity.ok(page.map(store -> StoreCardResponse.from(
                store, ratings.get(store.getId()), ranks.get(store.getId()), followerCounts.get(store.getId()),
                activeLiveIds.containsKey(store.getId()), categories.get(store.getId()))));
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
        return ResponseEntity.status(HttpStatus.CREATED).body(toStoreResponse(store));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<StoreResponse> getMyStore(@AuthenticationPrincipal UserPrincipal principal) {
        var store = getStoreUseCase.getByUserId(principal.getUserId());
        return ResponseEntity.ok(toStoreResponse(store));
    }

    @GetMapping("/{slug}")
    ResponseEntity<StoreCardResponse> getBySlug(@PathVariable String slug) {
        var store = getStoreUseCase.getBySlug(slug);
        var storeId = store.getId();
        var rating = loadRatingsSafely(Set.of(storeId)).get(storeId);
        var rank = loadRanksSafely(Set.of(storeId)).get(storeId);
        var followerCount = loadFollowerCountsSafely(Set.of(storeId)).get(storeId);
        var liveNow = loadLiveNowSafely(Set.of(storeId)).containsKey(storeId);
        var category = loadCategoriesSafely(List.of(store)).get(storeId);
        return ResponseEntity.ok(StoreCardResponse.from(store, rating, rank, followerCount, liveNow, category));
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
        return ResponseEntity.ok(toStoreResponse(store));
    }

    @PutMapping("/me/category")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<StoreResponse> setMyCategory(
            @RequestBody SetStoreCategoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var store = setStoreCategoryUseCase.setCategory(principal.getUserId(), request.categoryId());
        return ResponseEntity.ok(toStoreResponse(store));
    }

    @GetMapping("/plans")
    ResponseEntity<List<PlanResponse>> getPlans() {
        var plans = Arrays.stream(Plan.values())
                .map(PlanResponse::from)
                .toList();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/address-types")
    ResponseEntity<List<AddressTypeResponse>> getAddressTypes() {
        var types = Arrays.stream(AddressType.values())
                .map(AddressTypeResponse::from)
                .toList();
        return ResponseEntity.ok(types);
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
        return ResponseEntity.ok(toStoreResponse(store));
    }

    @PatchMapping("/me/close")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<Void> closeMyStore(@AuthenticationPrincipal UserPrincipal principal) {
        closeStoreTemporarilyUseCase.close(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/reopen")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<Void> reopenMyStore(@AuthenticationPrincipal UserPrincipal principal) {
        reopenStoreUseCase.reopen(principal.getUserId());
        return ResponseEntity.noContent().build();
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
        return ResponseEntity.ok(toStoreResponse(store));
    }

    @PostMapping("/{storeId}/follow")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<Void> followStore(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        followStoreUseCase.follow(storeId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{storeId}/follow")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<Void> unfollowStore(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        unfollowStoreUseCase.unfollow(storeId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{storeId}/followers/count")
    ResponseEntity<FollowerCountResponse> getFollowerCount(@PathVariable UUID storeId) {
        long count = getStoreFollowersUseCase.getFollowerCount(storeId);
        return ResponseEntity.ok(new FollowerCountResponse(count));
    }

    // No @PreAuthorize here: the route is permitAll (see SecurityConfig) so an anonymous
    // visitor's store card doesn't 401. Still requires auth for a real answer — principal
    // is null for anonymous requests, in which case we degrade to "not following" instead
    // of dereferencing a null userId.
    @GetMapping("/{storeId}/following")
    ResponseEntity<FollowingStatusResponse> isFollowing(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean following = principal != null && getStoreFollowersUseCase.isFollowing(storeId, principal.getUserId());
        return ResponseEntity.ok(new FollowingStatusResponse(following));
    }

    // Each store-card enrichment loader is isolated with its own try/catch (mirrors
    // StoreRankingJob's per-loader isolation in the analytics module): a failure in
    // one dimension must degrade only that field, not fail the whole public listing
    // or detail response through GlobalExceptionHandler's unhandled-Exception path.

    private Map<UUID, LoadStoreRatingPort.StoreRatingSummary> loadRatingsSafely(Collection<UUID> storeIds) {
        try {
            return loadStoreRatingPort.loadSummaries(storeIds);
        } catch (Exception e) {
            log.error("Failed to load rating summaries for {} stores; defaulting ratings for this response", storeIds.size(), e);
            return Map.of();
        }
    }

    private Map<UUID, Integer> loadRanksSafely(Collection<UUID> storeIds) {
        try {
            return loadStoreRankPort.loadRanks(storeIds);
        } catch (Exception e) {
            log.error("Failed to load ranks for {} stores; defaulting ranking position for this response", storeIds.size(), e);
            return Map.of();
        }
    }

    private Map<UUID, Long> loadFollowerCountsSafely(Collection<UUID> storeIds) {
        try {
            return getStoreFollowersUseCase.getFollowerCounts(storeIds);
        } catch (Exception e) {
            log.error("Failed to load follower counts for {} stores; defaulting follower count for this response", storeIds.size(), e);
            return Map.of();
        }
    }

    private Map<UUID, UUID> loadLiveNowSafely(Collection<UUID> storeIds) {
        try {
            return loadStoreLiveStatusPort.loadActiveLiveIds(storeIds);
        } catch (Exception e) {
            log.error("Failed to load live status for {} stores; defaulting liveNow to false for this response", storeIds.size(), e);
            return Map.of();
        }
    }

    // Every /me-style response goes through here so none of them reports category=null
    // just because it skipped resolution (the frontend would read that as "cleared").
    private StoreResponse toStoreResponse(Store store) {
        return StoreResponse.from(store, loadCategoriesSafely(List.of(store)).get(store.getId()));
    }

    // Effective category per store in at most 2 batched queries: manual overrides that are
    // still ACTIVE win (MANUAL); every other store (no override, or override since
    // deactivated) falls back to its dominant product category (INFERRED).
    private Map<UUID, StoreCategoryResponse> loadCategoriesSafely(Collection<Store> stores) {
        try {
            var overrideIds = stores.stream()
                    .map(Store::getCategoryId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            var manualRefs = storeCategoryPort.loadActiveByIds(overrideIds);

            Map<UUID, StoreCategoryResponse> result = new HashMap<>();
            Set<UUID> toInfer = new HashSet<>();
            for (Store store : stores) {
                var manual = store.getCategoryId() != null ? manualRefs.get(store.getCategoryId()) : null;
                if (manual != null) {
                    result.put(store.getId(), StoreCategoryResponse.of(manual, StoreCategoryResponse.Source.MANUAL));
                } else {
                    toInfer.add(store.getId());
                }
            }
            if (!toInfer.isEmpty()) {
                storeCategoryPort.inferTopByStore(toInfer).forEach((storeId, ref) ->
                        result.put(storeId, StoreCategoryResponse.of(ref, StoreCategoryResponse.Source.INFERRED)));
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to load categories for {} stores; defaulting category to null for this response", stores.size(), e);
            return Map.of();
        }
    }
}
