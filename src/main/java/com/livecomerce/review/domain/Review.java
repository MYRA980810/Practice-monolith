package com.livecomerce.review.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "description_accuracy_rating", nullable = false)
    private int descriptionAccuracyRating;

    @Column(name = "packaging_condition_rating", nullable = false)
    private int packagingConditionRating;

    @Column(name = "delivery_timeliness_rating", nullable = false)
    private int deliveryTimelinessRating;

    @Column(name = "seller_attention_rating", nullable = false)
    private int sellerAttentionRating;

    @Column(name = "ranking_impact_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal rankingImpactScore;

    @Column(length = 300)
    private String comment;

    @Column(nullable = false)
    private boolean anonymous;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ReviewPhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ProductRating> productRatings = new ArrayList<>();

    public static Review create(UUID orderId, UUID storeId, UUID buyerId,
                                 int descriptionAccuracyRating, int packagingConditionRating,
                                 int deliveryTimelinessRating, int sellerAttentionRating,
                                 String comment, boolean anonymous) {
        var review = new Review();
        review.orderId = orderId;
        review.storeId = storeId;
        review.buyerId = buyerId;
        review.descriptionAccuracyRating = descriptionAccuracyRating;
        review.packagingConditionRating = packagingConditionRating;
        review.deliveryTimelinessRating = deliveryTimelinessRating;
        review.sellerAttentionRating = sellerAttentionRating;
        review.rankingImpactScore = RankingWeights.computeScore(
                descriptionAccuracyRating, packagingConditionRating, deliveryTimelinessRating, sellerAttentionRating);
        review.comment = comment;
        review.anonymous = anonymous;
        review.createdAt = OffsetDateTime.now();
        return review;
    }

    public void addPhoto(String url, int position) {
        photos.add(ReviewPhoto.of(this, url, position));
    }

    public void addProductRating(UUID orderItemId, UUID productId, int rating) {
        productRatings.add(ProductRating.of(this, orderItemId, productId, rating));
    }

    public List<ReviewPhoto> getPhotos() {
        return Collections.unmodifiableList(photos);
    }

    public List<ProductRating> getProductRatings() {
        return Collections.unmodifiableList(productRatings);
    }
}
