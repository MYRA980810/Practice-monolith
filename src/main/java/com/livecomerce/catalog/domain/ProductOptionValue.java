package com.livecomerce.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.util.UUID;
import java.util.regex.Pattern;

@Entity
@Table(name = "product_option_values")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOptionValue implements Persistable<UUID> {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "option_id", nullable = false)
    private ProductOption option;

    @Column(nullable = false, length = 100)
    private String value;

    @Column(nullable = false)
    private int position;

    /**
     * Only populated for option values that represent a color (e.g. an option
     * named "Color"). Nullable by design: sizes, materials, etc. never carry
     * a swatch. Format is enforced as {@code #RRGGBB} when present.
     */
    @Column(name = "swatch_hex", length = 7)
    private String swatchHex;

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

    public void assignSwatchHex(String swatchHex) {
        if (swatchHex != null && !HEX_COLOR.matcher(swatchHex).matches()) {
            throw new IllegalArgumentException(
                    "swatchHex must match #RRGGBB, got: " + swatchHex);
        }
        this.swatchHex = swatchHex;
    }
}
