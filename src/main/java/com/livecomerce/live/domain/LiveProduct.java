package com.livecomerce.live.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LiveProduct {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_id", nullable = false)
    private Live live;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "product_name_snapshot", nullable = false, length = 255)
    private String productNameSnapshot;

    @Column(name = "price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceSnapshot;

    @Column(name = "currency_snapshot", nullable = false, length = 3)
    private String currencySnapshot;

    @Column(name = "stock_allocated", nullable = false)
    private int stockAllocated;

    @Column(name = "stock_sold", nullable = false)
    private int stockSold = 0;

    @Column(name = "is_hot", nullable = false)
    private boolean isHot = false;

    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned = false;

    @Column(name = "position", nullable = false)
    private int position = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static LiveProduct forCatalogProduct(Live live, UUID productId, UUID variantId,
                                                String nameSnapshot, BigDecimal priceSnapshot,
                                                String currency, int stockAllocated) {
        var lp = new LiveProduct();
        lp.id                  = UUID.randomUUID();
        lp.live                = live;
        lp.productId           = productId;
        lp.variantId           = variantId;
        lp.productNameSnapshot = nameSnapshot;
        lp.priceSnapshot       = priceSnapshot;
        lp.currencySnapshot    = currency;
        lp.stockAllocated      = stockAllocated;
        lp.stockSold           = 0;
        lp.isHot               = false;
        lp.isPinned            = false;
        lp.createdAt           = OffsetDateTime.now();
        lp.updatedAt           = OffsetDateTime.now();
        return lp;
    }

    public static LiveProduct forHotProduct(Live live, String name, BigDecimal price,
                                            String currency, int stockAllocated) {
        if (stockAllocated <= 0) {
            throw new IllegalArgumentException("Hot product must have stockAllocated > 0, got: " + stockAllocated);
        }
        var lp = new LiveProduct();
        lp.id                  = UUID.randomUUID();
        lp.live                = live;
        lp.productId           = null;
        lp.variantId           = null;
        lp.productNameSnapshot = name;
        lp.priceSnapshot       = price;
        lp.currencySnapshot    = currency;
        lp.stockAllocated      = stockAllocated;
        lp.stockSold           = 0;
        lp.isHot               = true;
        lp.isPinned            = false;
        lp.createdAt           = OffsetDateTime.now();
        lp.updatedAt           = OffsetDateTime.now();
        return lp;
    }

    public void pin() {
        this.isPinned  = true;
        this.updatedAt = OffsetDateTime.now();
    }

    public void unpin() {
        this.isPinned  = false;
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Domain-level guard: only for non-hot products. Hot products bypass stock limits here.
     * The actual atomic SQL check happens in the JPA repository.
     */
    public void tryAtomicStockReserve() {
        if (!isHot && stockSold >= stockAllocated) {
            throw new LiveProductOutOfStockException(this.id);
        }
    }

    public void incrementStockSold() {
        this.stockSold++;
        this.updatedAt = OffsetDateTime.now();
    }

    public void setPosition(int position) {
        this.position  = position;
        this.updatedAt = OffsetDateTime.now();
    }
}
