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
        boolean temporarilyClosed,
        OffsetDateTime createdAt,
        String shippingStreet,
        String shippingExtNumber,
        String shippingIntNumber,
        String shippingNeighborhood,
        String shippingCity,
        String shippingState,
        String shippingZipCode,
        String shippingCountry
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
                store.isTemporarilyClosed(),
                store.getCreatedAt(),
                store.getShippingStreet(),
                store.getShippingExtNumber(),
                store.getShippingIntNumber(),
                store.getShippingNeighborhood(),
                store.getShippingCity(),
                store.getShippingState(),
                store.getShippingZipCode(),
                store.getShippingCountry()
        );
    }
}
