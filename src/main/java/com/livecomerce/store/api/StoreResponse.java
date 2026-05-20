package com.livecomerce.store.api;

import com.livecomerce.store.domain.Store;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StoreResponse(
        UUID id,
        UUID userId,
        String name,
        String slug,
        String description,
        String logoUrl,
        PlanResponse plan,
        boolean active,
        boolean suspended,
        OffsetDateTime createdAt
) {
    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getUserId(),
                store.getName(),
                store.getSlug(),
                store.getDescription(),
                store.getLogoUrl(),
                PlanResponse.from(store.getPlan()),
                store.isActive(),
                store.isSuspended(),
                store.getCreatedAt()
        );
    }
}
