package com.livecomerce.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 100)
    private String sku;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Stock stock;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ProductImage> images = new ArrayList<>();

    @Transient
    private boolean isNew = false;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public static Product create(UUID storeId, String name, String description,
                                 BigDecimal basePrice, String currency, String sku) {
        var product = new Product();
        product.id          = UUID.randomUUID();
        product.isNew       = true;
        product.storeId     = storeId;
        product.name        = name;
        product.description = description;
        product.basePrice   = basePrice;
        product.currency    = currency != null ? currency : "MXN";
        product.sku         = sku;
        product.active      = true;
        product.createdAt   = OffsetDateTime.now();
        product.updatedAt   = OffsetDateTime.now();
        product.stock       = Stock.emptyFor(product);
        return product;
    }

    public void addImage(String url, int position, boolean primary) {
        images.add(ProductImage.of(this, url, position, primary));
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

    public void update(String name, String description, BigDecimal basePrice, String currency, String sku) {
        this.name        = name;
        this.description = description;
        this.basePrice   = basePrice;
        this.currency    = currency;
        this.sku         = sku;
        this.updatedAt   = OffsetDateTime.now();
    }

    public void updateImage(UUID imageId, String url, int position, boolean primary) {
        var image = images.stream()
                .filter(i -> i.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId));
        image.update(url, position, primary);
        this.updatedAt = OffsetDateTime.now();
    }

    public void removeImage(UUID imageId) {
        boolean removed = images.removeIf(i -> i.getId().equals(imageId));
        if (!removed) {
            throw new IllegalArgumentException("Image not found: " + imageId);
        }
        this.updatedAt = OffsetDateTime.now();
    }

    public void deactivate() {
        this.active    = false;
        this.updatedAt = OffsetDateTime.now();
    }

    public List<ProductImage> getImages() {
        return Collections.unmodifiableList(images);
    }
}
