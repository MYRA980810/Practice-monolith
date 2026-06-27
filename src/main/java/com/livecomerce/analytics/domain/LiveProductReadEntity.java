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
import java.util.UUID;

@Entity
@Immutable
@Table(name = "live_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LiveProductReadEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "live_id")
    private UUID liveId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_name_snapshot")
    private String productNameSnapshot;

    @Column(name = "price_snapshot")
    private BigDecimal priceSnapshot;

    @Column(name = "stock_allocated")
    private int stockAllocated;

    @Column(name = "stock_sold")
    private int stockSold;
}
