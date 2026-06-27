package com.livecomerce.store.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "store_followers")
@IdClass(StoreFollowerId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreFollower {

    @Id
    @Column(name = "store_id")
    private UUID storeId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static StoreFollower create(UUID storeId, UUID userId) {
        var f = new StoreFollower();
        f.storeId = storeId;
        f.userId = userId;
        f.createdAt = OffsetDateTime.now();
        return f;
    }
}
