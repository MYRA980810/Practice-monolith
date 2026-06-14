package com.livecomerce.catalog.domain;

import com.livecomerce.catalog.application.DuplicateVariantException;
import com.livecomerce.catalog.application.ProductCannotBePausedException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

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

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean paused = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "category_id")
    private UUID categoryId;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ProductOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = 20)
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

    @Override
    public UUID getId() {
        return id;
    }

    public static Product create(UUID storeId, String name, String description,
                                 BigDecimal basePrice, String currency, String sku, UUID categoryId) {
        var product = new Product();
        product.id          = UUID.randomUUID();
        product.isNew       = true;
        product.storeId     = storeId;
        product.name        = name;
        product.description = description;
        product.basePrice   = basePrice;
        product.currency    = currency != null ? currency : "MXN";
        product.categoryId  = categoryId;
        product.active      = true;
        product.createdAt   = OffsetDateTime.now();
        product.updatedAt   = OffsetDateTime.now();
        product.variants.add(ProductVariant.createDefault(product, sku));
        return product;
    }

    public ProductVariant defaultVariant() {
        return variants.stream()
                .filter(ProductVariant::isDefault)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No default variant"));
    }

    public void assignCategory(UUID categoryId) {
        this.categoryId = categoryId;
        this.updatedAt  = OffsetDateTime.now();
    }

    public ProductOption addOption(String name, List<String> values) {
        var option = ProductOption.of(this, name, options.size(), values);
        options.add(option);
        this.updatedAt = OffsetDateTime.now();
        return option;
    }

    public ProductVariant addVariant(Map<String, String> selection, String sku, BigDecimal priceOverride) {
        var selectedValues = new HashSet<ProductOptionValue>();
        for (var option : options) {
            var valueName = selection.get(option.getName());
            if (valueName == null)
                throw new IllegalArgumentException("Missing option: " + option.getName());
            selectedValues.add(option.findValue(valueName));
        }

        for (var existing : variants) {
            if (!existing.isDefault() && existing.getOptionValues().equals(selectedValues))
                throw new DuplicateVariantException(id, selection);
        }

        var variant = ProductVariant.create(this, sku, priceOverride, selectedValues, variants.size());
        variants.add(variant);
        this.updatedAt = OffsetDateTime.now();
        return variant;
    }

    public void addStock(int qty) {
        defaultVariant().addStock(qty);
        this.updatedAt = OffsetDateTime.now();
    }

    public void reserveStock(int qty) {
        defaultVariant().reserveStock(qty);
        this.updatedAt = OffsetDateTime.now();
    }

    public void releaseStock(int qty) {
        defaultVariant().releaseStock(qty);
        this.updatedAt = OffsetDateTime.now();
    }

    public void sellStock(int qty) {
        defaultVariant().sellStock(qty);
        this.updatedAt = OffsetDateTime.now();
    }

    public void update(String name, String description, BigDecimal basePrice, String currency, String sku) {
        this.name        = name;
        this.description = description;
        this.basePrice   = basePrice;
        this.currency    = currency;
        defaultVariant().updateSku(sku);
        this.updatedAt   = OffsetDateTime.now();
    }

    public void addImage(String url, Integer position, Boolean primary) {
        boolean isPrimary;
        int pos;
        if (images.isEmpty()) {
            isPrimary = true;
            pos = 0;
        } else {
            isPrimary = Boolean.TRUE.equals(primary);
            pos = position != null ? position : images.size();
        }
        if (isPrimary) {
            images.stream()
                .filter(ProductImage::isPrimary)
                .forEach(i -> i.update(i.getUrl(), i.getPosition(), false));
        }
        images.add(ProductImage.of(this, url, pos, isPrimary));
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateImage(UUID imageId, String url, Integer position, Boolean primary) {
        var image = images.stream()
                .filter(i -> i.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId));
        boolean isPrimary = Boolean.TRUE.equals(primary);
        int pos = position != null ? position : image.getPosition();
        if (isPrimary) {
            images.stream()
                .filter(i -> !i.getId().equals(imageId) && i.isPrimary())
                .forEach(i -> i.update(i.getUrl(), i.getPosition(), false));
        }
        image.update(url, pos, isPrimary);
        this.updatedAt = OffsetDateTime.now();
    }

    public void removeImage(UUID imageId) {
        var image = images.stream()
                .filter(i -> i.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId));
        boolean wasPrimary = image.isPrimary();
        images.remove(image);
        if (wasPrimary && !images.isEmpty()) {
            var first = images.getFirst();
            first.update(first.getUrl(), first.getPosition(), true);
        }
        this.updatedAt = OffsetDateTime.now();
    }

    public void pause() {
        if (!this.active) {
            throw new ProductCannotBePausedException(this.id);
        }
        this.paused    = true;
        this.updatedAt = OffsetDateTime.now();
    }

    public void resume() {
        this.paused    = false;
        this.updatedAt = OffsetDateTime.now();
    }

    public void deactivate() {
        this.active    = false;
        this.updatedAt = OffsetDateTime.now();
    }

    public List<ProductOption> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public List<ProductVariant> getVariants() {
        return Collections.unmodifiableList(variants);
    }

    public List<ProductImage> getImages() {
        return Collections.unmodifiableList(images);
    }
}
