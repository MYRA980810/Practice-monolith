package com.livecomerce.billing.domain;

import com.livecomerce.shared.Plan;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, length = 10)
    private Plan plan;

    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(nullable = false, length = 20)
    private Gateway gateway;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "current_period_start", nullable = false)
    private OffsetDateTime currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private OffsetDateTime currentPeriodEnd;

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

    public static Subscription create(UUID userId, Plan plan, Gateway gateway,
                                      String externalId, OffsetDateTime periodEnd) {
        var s = new Subscription();
        s.id                 = UUID.randomUUID();
        s.isNew              = true;
        s.userId             = userId;
        s.plan               = plan;
        s.status             = SubscriptionStatus.ACTIVE;
        s.gateway            = gateway;
        s.externalId         = externalId;
        s.currentPeriodStart = OffsetDateTime.now();
        s.currentPeriodEnd   = periodEnd;
        s.createdAt          = OffsetDateTime.now();
        s.updatedAt          = OffsetDateTime.now();
        return s;
    }

    public void renew(String externalId, OffsetDateTime newPeriodEnd) {
        this.externalId         = externalId;
        this.status             = SubscriptionStatus.ACTIVE;
        this.currentPeriodStart = OffsetDateTime.now();
        this.currentPeriodEnd   = newPeriodEnd;
        this.updatedAt          = OffsetDateTime.now();
    }

    public void expire() {
        this.status    = SubscriptionStatus.EXPIRED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void cancel() {
        this.status    = SubscriptionStatus.CANCELLED;
        this.updatedAt = OffsetDateTime.now();
    }
}
