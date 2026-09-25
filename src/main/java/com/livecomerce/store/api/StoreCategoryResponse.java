package com.livecomerce.store.api;

import com.livecomerce.store.StoreCategoryPort.CategoryRef;

import java.util.UUID;

/**
 * Effective store category: the seller's manual override when it points to an active
 * category, otherwise the category inferred from the store's active products.
 */
public record StoreCategoryResponse(
        UUID id,
        String name,
        String slug,
        Source source
) {
    public enum Source { MANUAL, INFERRED }

    static StoreCategoryResponse of(CategoryRef ref, Source source) {
        return new StoreCategoryResponse(ref.id(), ref.name(), ref.slug(), source);
    }
}
