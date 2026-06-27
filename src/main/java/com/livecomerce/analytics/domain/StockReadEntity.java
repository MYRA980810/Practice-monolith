package com.livecomerce.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReadEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "total_quantity")
    private int totalQuantity;

    @Column(name = "available_quantity")
    private int availableQuantity;

    @Column(name = "reserved_quantity")
    private int reservedQuantity;
}
