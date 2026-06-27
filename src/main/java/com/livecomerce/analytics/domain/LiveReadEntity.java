package com.livecomerce.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "lives")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LiveReadEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "title")
    private String title;

    @Column(name = "status")
    private String status;

    @Column(name = "peak_viewers")
    private int peakViewers;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
