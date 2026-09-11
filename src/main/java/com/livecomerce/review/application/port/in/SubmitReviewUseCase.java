package com.livecomerce.review.application.port.in;

import com.livecomerce.review.domain.Review;

import java.util.List;
import java.util.UUID;

public interface SubmitReviewUseCase {

    record ProductRatingInput(UUID orderItemId, int rating) {}

    record SubmitReviewCommand(
            UUID orderId,
            UUID buyerId,
            int descriptionAccuracyRating,
            int packagingConditionRating,
            int deliveryTimelinessRating,
            int sellerAttentionRating,
            String comment,
            boolean anonymous,
            List<String> photoUrls,
            List<ProductRatingInput> productRatings
    ) {}

    Review submitReview(SubmitReviewCommand command);
}
