package com.livecomerce.store.api;

import com.livecomerce.store.domain.Store;

public record StoreCardResponse(
        String name,
        String slug,
        String description,
        String logoUrl
) {
    public static StoreCardResponse from(Store store) {
        return new StoreCardResponse(
                store.getName(),
                store.getSlug(),
                store.getDescription(),
                store.getLogoUrl()
        );
    }
}
