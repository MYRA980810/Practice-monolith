package com.livecomerce.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption implements Persistable<UUID> {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "option", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<ProductOptionValue> values = new ArrayList<>();

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

    static ProductOption of(Product product, String name, int position, List<String> valueNames) {
        var option = new ProductOption();
        option.id        = UUID.randomUUID();
        option.product   = product;
        option.name      = name;
        option.position  = position;
        option.createdAt = OffsetDateTime.now();
        option.isNew     = true;

        for (int i = 0; i < valueNames.size(); i++) {
            option.values.add(ProductOptionValue.of(option, valueNames.get(i), i));
        }
        return option;
    }

    public ProductOptionValue findValue(String value) {
        return values.stream()
                .filter(v -> v.getValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Option value not found: " + value + " in option: " + name));
    }

    public List<ProductOptionValue> getValues() {
        return Collections.unmodifiableList(values);
    }
}
