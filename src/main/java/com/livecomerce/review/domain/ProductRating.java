package com.livecomerce.review.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "product_ratings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductRating {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_order_id", nullable = false)
    private Review review;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int rating;

    static ProductRating of(Review review, UUID orderItemId, UUID productId, int rating) {
        var pr = new ProductRating();
        pr.id = UUID.randomUUID();
        pr.review = review;
        pr.orderItemId = orderItemId;
        pr.productId = productId;
        pr.rating = rating;
        return pr;
    }
}
