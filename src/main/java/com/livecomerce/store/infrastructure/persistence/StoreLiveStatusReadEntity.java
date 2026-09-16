package com.livecomerce.store.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Local event-driven projection of "this store currently has an active live",
 * upserted by {@link com.livecomerce.store.application.StoreLiveStatusEventListener}
 * from {@code live}'s LiveStartedEvent/LiveEndedEvent so store-card/listing reads
 * never cross into the {@code live} module at request time. One row per store:
 * assumes a store runs at most one live at a time.
 *
 * <p>The row is never deleted — {@code live} carries the current status and
 * {@code lastEventAt} (the event's own {@code startedAt}/{@code endedAt}, not
 * processing time) is compared against every incoming event so an out-of-order
 * redelivery (an at-least-once retry of a stale {@code LiveStartedEvent} landing
 * after its {@code LiveEndedEvent} already applied, or vice versa) is detected
 * and ignored instead of resurrecting a stale status. Both events key off {@code
 * storeId}, so whichever actually arrives first creates the row and the other
 * is compared against it regardless of arrival order.
 *
 * <p>{@code storeId} is manually assigned (never {@code @GeneratedValue}), so
 * {@code JpaRepository.save} resolves to an upsert via {@code EntityManager.merge}
 * regardless of whether a row already exists for that store.
 */
@Entity
@Table(name = "store_live_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class StoreLiveStatusReadEntity {

    @Id
    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "live_id")
    private UUID liveId;

    @Column(name = "is_live", nullable = false)
    private boolean live;

    @Column(name = "last_event_at", nullable = false)
    private Instant lastEventAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** First row ever seen for this store, created by a LiveStartedEvent. */
    static StoreLiveStatusReadEntity ofStarted(UUID storeId, UUID liveId, Instant occurredAt) {
        return of(storeId, liveId, true, occurredAt);
    }

    /** First row ever seen for this store, created by a LiveEndedEvent that outraced its start. */
    static StoreLiveStatusReadEntity ofEnded(UUID storeId, UUID liveId, Instant occurredAt) {
        return of(storeId, liveId, false, occurredAt);
    }

    private static StoreLiveStatusReadEntity of(UUID storeId, UUID liveId, boolean live, Instant occurredAt) {
        var entity = new StoreLiveStatusReadEntity();
        entity.storeId = storeId;
        entity.liveId = liveId;
        entity.live = live;
        entity.lastEventAt = occurredAt;
        entity.updatedAt = OffsetDateTime.now();
        return entity;
    }

    /**
     * Applies a {@code LiveStartedEvent}. Ignored (returns {@code false}) if
     * {@code occurredAt} is older than the last event already applied — the
     * signature of a delayed retry arriving after a later event.
     */
    boolean applyStarted(UUID liveId, Instant occurredAt) {
        if (occurredAt.isBefore(this.lastEventAt)) {
            return false;
        }
        this.liveId = liveId;
        this.live = true;
        this.lastEventAt = occurredAt;
        this.updatedAt = OffsetDateTime.now();
        return true;
    }

    /**
     * Applies a {@code LiveEndedEvent}. Ignored (returns {@code false}) under
     * the same stale-retry condition as {@link #applyStarted}.
     */
    boolean applyEnded(UUID liveId, Instant occurredAt) {
        if (occurredAt.isBefore(this.lastEventAt)) {
            return false;
        }
        this.liveId = liveId;
        this.live = false;
        this.lastEventAt = occurredAt;
        this.updatedAt = OffsetDateTime.now();
        return true;
    }
}
