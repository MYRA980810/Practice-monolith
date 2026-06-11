package com.livecomerce.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "product_variants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariant implements Persistable<UUID> {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 100)
    private String sku;

    @Column(name = "price_override", precision = 10, scale = 2)
    private BigDecimal priceOverride;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToOne(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Stock stock;

    @ManyToMany
    @JoinTable(
            name = "product_variant_options",
            joinColumns = @JoinColumn(name = "variant_id"),
            inverseJoinColumns = @JoinColumn(name = "option_value_id")
    )
    private Set<ProductOptionValue> optionValues = new HashSet<>();

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public UUID getId() {
        return id;
    }

    static ProductVariant createDefault(Product product, String sku) {
        var variant = new ProductVariant();
        variant.id          = UUID.randomUUID();
        variant.product     = product;
        variant.sku         = sku;
        variant.isDefault   = true;
        variant.position    = 0;
        variant.createdAt   = OffsetDateTime.now();
        variant.updatedAt   = OffsetDateTime.now();
        variant.isNew       = true;
        variant.stock       = Stock.emptyFor(variant);
        return variant;
    }

    static ProductVariant create(Product product, String sku, BigDecimal priceOverride,
                                  Set<ProductOptionValue> optionValues, int position) {
        var variant = new ProductVariant();
        variant.id            = UUID.randomUUID();
        variant.product       = product;
        variant.sku           = sku;
        variant.priceOverride = priceOverride;
        variant.isDefault     = false;
        variant.position      = position;
        variant.createdAt     = OffsetDateTime.now();
        variant.updatedAt     = OffsetDateTime.now();
        variant.isNew         = true;
        variant.optionValues  = new HashSet<>(optionValues);
        variant.stock         = Stock.emptyFor(variant);
        return variant;
    }

    public BigDecimal effectivePrice(BigDecimal basePrice) {
        return priceOverride != null ? priceOverride : basePrice;
    }

    public void updateSku(String sku) {
        this.sku       = sku;
        this.updatedAt = OffsetDateTime.now();
    }

    public void addStock(int qty) {
        this.stock.add(qty);
        this.updatedAt = OffsetDateTime.now();
    }

    public void reserveStock(int qty) {
        this.stock.reserve(qty);
        this.updatedAt = OffsetDateTime.now();
    }

    public void releaseStock(int qty) {
        this.stock.release(qty);
        this.updatedAt = OffsetDateTime.now();
    }

    public void sellStock(int qty) {
        this.stock.sell(qty);
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean canReserve(int qty) {
        return this.stock.canReserve(qty);
    }

    public Set<ProductOptionValue> getOptionValues() {
        return optionValues;
    }
}
