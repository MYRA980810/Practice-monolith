package com.livecomerce.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "seller_connect_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerConnectAccount implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "stripe_account_id", nullable = false, length = 255)
    private String stripeAccountId;

    @Column(name = "charges_enabled", nullable = false)
    private boolean chargesEnabled = false;

    @Column(name = "payouts_enabled", nullable = false)
    private boolean payoutsEnabled = false;

    @Column(name = "details_submitted", nullable = false)
    private boolean detailsSubmitted = false;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

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

    public static SellerConnectAccount create(UUID userId, String stripeAccountId) {
        var account = new SellerConnectAccount();
        account.id               = UUID.randomUUID();
        account.isNew            = true;
        account.userId           = userId;
        account.stripeAccountId  = stripeAccountId;
        account.chargesEnabled   = false;
        account.payoutsEnabled   = false;
        account.detailsSubmitted = false;
        account.activatedAt      = null;
        account.createdAt        = OffsetDateTime.now();
        account.updatedAt        = OffsetDateTime.now();
        return account;
    }

    public void updateCapabilities(boolean chargesEnabled, boolean payoutsEnabled, boolean detailsSubmitted) {
        boolean wasInactive = this.activatedAt == null;

        this.chargesEnabled   = chargesEnabled;
        this.payoutsEnabled   = payoutsEnabled;
        this.detailsSubmitted = detailsSubmitted;
        this.updatedAt        = OffsetDateTime.now();

        if (wasInactive && chargesEnabled && payoutsEnabled && detailsSubmitted) {
            this.activatedAt = OffsetDateTime.now();
        }
    }
}
