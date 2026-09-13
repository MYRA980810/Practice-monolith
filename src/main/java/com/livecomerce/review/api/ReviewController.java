package com.livecomerce.review.api;

import com.livecomerce.review.application.port.in.ListProductReviewsUseCase;
import com.livecomerce.review.application.port.in.ListStoreReviewsUseCase;
import com.livecomerce.review.application.port.in.SubmitReviewUseCase;
import com.livecomerce.shared.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
class ReviewController {

    private final SubmitReviewUseCase submitReviewUseCase;
    private final ListStoreReviewsUseCase listStoreReviewsUseCase;
    private final ListProductReviewsUseCase listProductReviewsUseCase;

    @PostMapping("/api/orders/{orderId}/review")
    @PreAuthorize("hasRole('BUYER')")
    ResponseEntity<ReviewResponse> submitReview(
            @PathVariable UUID orderId,
            @Valid @RequestBody SubmitReviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var productRatings = request.productRatings().stream()
                .map(r -> new SubmitReviewUseCase.ProductRatingInput(r.orderItemId(), r.rating()))
                .toList();

        var review = submitReviewUseCase.submitReview(new SubmitReviewUseCase.SubmitReviewCommand(
                orderId,
                principal.getUserId(),
                request.descriptionAccuracyRating(),
                request.packagingConditionRating(),
                request.deliveryTimelinessRating(),
                request.sellerAttentionRating(),
                request.comment(),
                request.anonymous(),
                request.photoUrls(),
                productRatings
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(ReviewResponse.from(review));
    }

    @GetMapping("/api/stores/{storeId}/reviews")
    ResponseEntity<Page<StoreReviewResponse>> listStoreReviews(
            @PathVariable UUID storeId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(listStoreReviewsUseCase.listByStore(storeId, pageable).map(StoreReviewResponse::from));
    }

    @GetMapping("/api/products/{id}/reviews")
    ResponseEntity<Page<ProductReviewResponse>> listProductReviews(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(listProductReviewsUseCase.listByProduct(id, pageable).map(ProductReviewResponse::from));
    }
}
