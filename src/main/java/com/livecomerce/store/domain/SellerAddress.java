package com.livecomerce.store.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "seller_addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerAddress implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String street;

    @Column(name = "ext_number", length = 20)
    private String extNumber;

    @Column(name = "int_number", length = 20)
    private String intNumber;

    @Column(length = 100)
    private String neighborhood;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;

    @Column(nullable = false, length = 3)
    private String country;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

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

    public static SellerAddress create(UUID userId, String street, String extNumber,
            String intNumber, String neighborhood, String city, String state,
            String zipCode, String country, Double latitude, Double longitude) {
        var address = new SellerAddress();
        address.id           = UUID.randomUUID();
        address.isNew        = true;
        address.userId       = userId;
        address.street       = street;
        address.extNumber    = extNumber;
        address.intNumber    = intNumber;
        address.neighborhood = neighborhood;
        address.city         = city;
        address.state        = state;
        address.zipCode      = zipCode;
        address.country      = country;
        address.latitude     = latitude;
        address.longitude    = longitude;
        address.isDefault    = false;
        address.createdAt    = OffsetDateTime.now();
        return address;
    }

    public void setAsDefault() {
        this.isDefault = true;
    }

    public void removeDefault() {
        this.isDefault = false;
    }
}
