package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.Category;

import java.util.UUID;

public interface CreateCategoryUseCase {
    record CreateCategoryCommand(String name, String slug, UUID createdBy) {}
    Category create(CreateCategoryCommand command);
}
