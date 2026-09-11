package com.livecomerce.review.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The 4 "Livento ranking criteria" weights shown to the buyer in the rating form
 * (30% / 20% / 25% / 25%). Fixed domain constants, not runtime config — the
 * percentages are printed literally in the buyer-facing UI, so changing them
 * requires a coordinated frontend release regardless.
 */
public final class RankingWeights {

    public static final BigDecimal DESCRIPTION_ACCURACY = new BigDecimal("0.30");
    public static final BigDecimal PACKAGING_CONDITION   = new BigDecimal("0.20");
    public static final BigDecimal DELIVERY_TIMELINESS   = new BigDecimal("0.25");
    public static final BigDecimal SELLER_ATTENTION      = new BigDecimal("0.25");

    private RankingWeights() {}

    public static BigDecimal computeScore(int descriptionAccuracyRating, int packagingConditionRating,
                                           int deliveryTimelinessRating, int sellerAttentionRating) {
        return DESCRIPTION_ACCURACY.multiply(BigDecimal.valueOf(descriptionAccuracyRating))
                .add(PACKAGING_CONDITION.multiply(BigDecimal.valueOf(packagingConditionRating)))
                .add(DELIVERY_TIMELINESS.multiply(BigDecimal.valueOf(deliveryTimelinessRating)))
                .add(SELLER_ATTENTION.multiply(BigDecimal.valueOf(sellerAttentionRating)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
