package com.livecomerce.store.domain;

import com.livecomerce.shared.Plan;
import com.livecomerce.store.application.StoreCannotBeReactivatedException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(nullable = false, length = 10)
    private Plan plan;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean suspended = false;

    @Column(name = "suspension_reason", length = 20)
    @Enumerated(EnumType.STRING)
    private SuspensionReason suspensionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

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

    public static Store create(UUID userId, String name, String slug, String description, String logoUrl) {
        var store = new Store();
        store.id             = UUID.randomUUID();
        store.isNew          = true;
        store.userId         = userId;
        store.name           = name;
        store.slug           = slug;
        store.description    = description;
        store.logoUrl        = logoUrl;
        store.plan            = Plan.FREE;
        store.commissionRate  = new BigDecimal("0.0500");
        store.active          = true;
        store.suspended       = false;
        store.createdAt       = OffsetDateTime.now();
        store.updatedAt       = OffsetDateTime.now();
        return store;
    }

    public void updateProfile(String name, String description, String logoUrl) {
        this.name        = name;
        this.description = description;
        this.logoUrl     = logoUrl;
        this.updatedAt   = OffsetDateTime.now();
    }

    public void changePlan(Plan newPlan) {
        this.plan           = newPlan;
        this.commissionRate = newPlan.getCommissionRate();
        this.updatedAt      = OffsetDateTime.now();
    }

    public void deactivate() {
        this.active    = false;
        this.updatedAt = OffsetDateTime.now();
    }

    public void reactivate() {
        if (this.suspended) {
            throw new StoreCannotBeReactivatedException();
        }
        this.active    = true;
        this.updatedAt = OffsetDateTime.now();
    }

    public void suspend(SuspensionReason reason) {
        this.suspended        = true;
        this.suspensionReason = reason;
        this.updatedAt        = OffsetDateTime.now();
    }

    public void unsuspend() {
        this.suspended        = false;
        this.suspensionReason = null;
        this.updatedAt        = OffsetDateTime.now();
    }
}
