package com.livecomerce.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Entity
@Table(name = "product_option_values")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOptionValue implements Persistable<UUID> {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "option_id", nullable = false)
    private ProductOption option;

    @Column(nullable = false, length = 100)
    private String value;

    @Column(nullable = false)
    private int position;

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

    static ProductOptionValue of(ProductOption option, String value, int position) {
        var v = new ProductOptionValue();
        v.id       = UUID.randomUUID();
        v.option   = option;
        v.value    = value;
        v.position = position;
        v.isNew    = true;
        return v;
    }
}
