package com.livecomerce.review.application.query;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReviewView(
        UUID orderId,
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
        List<ProductRatingInfo> productRatings,
        OffsetDateTime createdAt
) {
    public record ProductRatingInfo(UUID productId, int rating) {}
}
