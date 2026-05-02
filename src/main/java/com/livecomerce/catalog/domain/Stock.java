package com.livecomerce.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    Product product;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    static Stock emptyFor(Product product) {
        var stock = new Stock();
        stock.id                = UUID.randomUUID();
        stock.product           = product;
        stock.totalQuantity     = 0;
        stock.availableQuantity = 0;
        stock.reservedQuantity  = 0;
        stock.createdAt         = OffsetDateTime.now();
        stock.updatedAt         = OffsetDateTime.now();
        return stock;
    }

    public void add(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive");
        this.totalQuantity     += qty;
        this.availableQuantity += qty;
        this.updatedAt          = OffsetDateTime.now();
    }

    public boolean canReserve(int qty) {
        return availableQuantity >= qty;
    }

    public void reserve(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (!canReserve(qty)) throw new IllegalStateException(
                "Insufficient stock: requested=%d, available=%d".formatted(qty, availableQuantity));
        this.availableQuantity -= qty;
        this.reservedQuantity  += qty;
        this.updatedAt          = OffsetDateTime.now();
    }

    public void release(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive");
        this.reservedQuantity  -= qty;
        this.availableQuantity += qty;
        this.updatedAt          = OffsetDateTime.now();
    }

    public void sell(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive");
        this.reservedQuantity -= qty;
        this.totalQuantity    -= qty;
        this.updatedAt         = OffsetDateTime.now();
    }
}
