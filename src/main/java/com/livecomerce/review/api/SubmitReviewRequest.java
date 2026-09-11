package com.livecomerce.review.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SubmitReviewRequest(

        @Min(1) @Max(5)
        int descriptionAccuracyRating,

        @Min(1) @Max(5)
        int packagingConditionRating,

        @Min(1) @Max(5)
        int deliveryTimelinessRating,

        @Min(1) @Max(5)
        int sellerAttentionRating,

        @Size(max = 300)
        String comment,

        boolean anonymous,

        @Size(max = 10)
        List<@Size(max = 500) String> photoUrls,

        @NotEmpty
        List<@Valid ProductRatingRequest> productRatings
) {
    public record ProductRatingRequest(
            @NotNull UUID orderItemId,
            @Min(1) @Max(5) int rating
    ) {}
}
