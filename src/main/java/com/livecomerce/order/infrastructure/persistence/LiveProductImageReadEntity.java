package com.livecomerce.order.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Read-only view over the shared {@code live_products} table, scoped to the
 * columns needed to enrich order items with an image, without importing the
 * {@code live} module's domain (mirrors {@code analytics.LiveProductReadEntity}).
 */
@Entity
@Immutable
@Table(name = "live_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class LiveProductImageReadEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "live_id")
    private UUID liveId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "image_url")
    private String imageUrl;
}
