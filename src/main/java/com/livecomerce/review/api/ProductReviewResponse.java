package com.livecomerce.review.api;

import com.livecomerce.review.application.query.ReviewView;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public shape for {@code GET /api/products/{id}/reviews} ({@code permitAll}, same
 * as its sibling {@code GET /api/stores/{storeId}/reviews}). Deliberately omits
 * {@code orderId} for the same reason as {@link StoreReviewResponse}: it was never
 * masked for anonymous reviews and would let anyone deanonymize the buyer via
 * {@code GET /api/orders/{id}}.
 */
public record ProductReviewResponse(
        UUID storeId,
        String buyerDisplayName,
        int descriptionAccuracyRating,
        int packagingConditionRating,
        int deliveryTimelinessRating,
        int sellerAttentionRating,
        BigDecimal rankingImpactScore,
        String comment,
        boolean anonymous,
        List<String> photoUrls,
        List<ReviewView.ProductRatingInfo> productRatings,
        OffsetDateTime createdAt
) {
    public static ProductReviewResponse from(ReviewView view) {
        return new ProductReviewResponse(
                view.storeId(),
                view.buyerDisplayName(),
                view.descriptionAccuracyRating(),
                view.packagingConditionRating(),
                view.deliveryTimelinessRating(),
                view.sellerAttentionRating(),
                view.rankingImpactScore(),
                view.comment(),
                view.anonymous(),
                view.photoUrls(),
                view.productRatings(),
                view.createdAt()
        );
    }
}
