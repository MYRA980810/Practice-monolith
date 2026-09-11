package com.livecomerce.review.api;

import com.livecomerce.review.domain.Review;
import com.livecomerce.review.domain.ReviewPhoto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
        UUID orderId,
        UUID storeId,
        UUID buyerId,
        int descriptionAccuracyRating,
        int packagingConditionRating,
        int deliveryTimelinessRating,
        int sellerAttentionRating,
        BigDecimal rankingImpactScore,
        String comment,
        boolean anonymous,
        List<String> photoUrls,
        List<ProductRatingResponse> productRatings,
        OffsetDateTime createdAt
) {
    public record ProductRatingResponse(UUID orderItemId, UUID productId, int rating) {}

    public static ReviewResponse from(Review review) {
        var photoUrls = review.getPhotos().stream()
                .sorted(Comparator.comparingInt(ReviewPhoto::getPosition))
                .map(ReviewPhoto::getUrl)
                .toList();
        var productRatings = review.getProductRatings().stream()
                .map(pr -> new ProductRatingResponse(pr.getOrderItemId(), pr.getProductId(), pr.getRating()))
                .toList();
        return new ReviewResponse(
                review.getOrderId(),
                review.getStoreId(),
                review.getBuyerId(),
                review.getDescriptionAccuracyRating(),
                review.getPackagingConditionRating(),
                review.getDeliveryTimelinessRating(),
                review.getSellerAttentionRating(),
                review.getRankingImpactScore(),
                review.getComment(),
                review.isAnonymous(),
                photoUrls,
                productRatings,
                review.getCreatedAt()
        );
    }
}
