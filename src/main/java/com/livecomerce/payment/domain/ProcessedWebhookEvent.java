package com.livecomerce.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stripe_webhook_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedWebhookEvent implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "stripe_event_id", nullable = false, length = 255)
    private String stripeEventId;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

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

    public static ProcessedWebhookEvent create(String stripeEventId, String type) {
        var event = new ProcessedWebhookEvent();
        event.id            = UUID.randomUUID();
        event.isNew         = true;
        event.stripeEventId = stripeEventId;
        event.type          = type;
        event.processedAt   = OffsetDateTime.now();
        return event;
    }
}
