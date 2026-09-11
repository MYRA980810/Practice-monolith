package com.livecomerce.review.api;

import com.livecomerce.review.application.query.ReviewView;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public shape for {@code GET /api/stores/{storeId}/reviews} ({@code permitAll}).
 * Deliberately omits {@code orderId}: it is not needed for display and, unlike
 * {@code buyerDisplayName}, was never masked for anonymous reviews — leaking it here
 * let anyone read a real orderId off a public review and use it to deanonymize the
 * buyer via {@code GET /api/orders/{id}}. Keep {@code orderId} in the internal
 * {@link ReviewView} (used server-side); cut it only from this outward-facing DTO.
 */
public record StoreReviewResponse(
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
    public static StoreReviewResponse from(ReviewView view) {
        return new StoreReviewResponse(
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
