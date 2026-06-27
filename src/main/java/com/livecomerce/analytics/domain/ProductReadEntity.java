package com.livecomerce.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductReadEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "name")
    private String name;

    @Column(name = "base_price")
    private BigDecimal basePrice;

    @Column(name = "currency")
    private String currency;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "paused")
    private boolean paused;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
