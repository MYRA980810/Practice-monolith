package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.Category;

import java.util.List;
import java.util.UUID;

public interface ListCategoriesUseCase {
    List<Category> listActive();
    List<Category> getCategoriesInUse(UUID storeId);
}
