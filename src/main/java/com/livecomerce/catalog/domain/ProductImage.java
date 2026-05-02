package com.livecomerce.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false)
    private int position;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    static ProductImage of(Product product, String url, int position, boolean primary) {
        var image = new ProductImage();
        image.id        = UUID.randomUUID();
        image.product   = product;
        image.url       = url;
        image.position  = position;
        image.primary   = primary;
        image.createdAt = OffsetDateTime.now();
        return image;
    }
}
