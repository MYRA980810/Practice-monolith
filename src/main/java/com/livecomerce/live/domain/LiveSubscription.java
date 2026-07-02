package com.livecomerce.live.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LiveSubscription implements Persistable<LiveSubscriptionId> {

    @EmbeddedId
    private LiveSubscriptionId id;

    /**
     * Read-only navigation association. The live_id column is managed by the
     * embedded primary key — insertable and updatable are false here to avoid
     * the dual-mapping conflict.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_id", insertable = false, updatable = false)
    private Live live;

    @Column(name = "subscribed_at", nullable = false)
    private OffsetDateTime subscribedAt;

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

    public static LiveSubscription create(UUID liveId, UUID buyerId) {
        var sub = new LiveSubscription();
        sub.id = new LiveSubscriptionId(liveId, buyerId);
        sub.isNew = true;
        sub.subscribedAt = OffsetDateTime.now();
        return sub;
    }

    public UUID getLiveId() {
        return id.getLiveId();
    }

    public UUID getBuyerId() {
        return id.getBuyerId();
    }
}
