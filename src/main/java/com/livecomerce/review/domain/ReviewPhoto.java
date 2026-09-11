package com.livecomerce.review.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "review_photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewPhoto {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_order_id", nullable = false)
    private Review review;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false)
    private int position;

    static ReviewPhoto of(Review review, String url, int position) {
        var photo = new ReviewPhoto();
        photo.id = UUID.randomUUID();
        photo.review = review;
        photo.url = url;
        photo.position = position;
        return photo;
    }
}
