package com.livecomerce.catalog.api;

import com.livecomerce.catalog.domain.Category;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, String slug, Integer displayOrder, boolean featured) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(),
                category.getDisplayOrder(), category.isFeatured());
    }
}
