package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.Category;

import java.util.List;

public interface ListCategoriesUseCase {
    List<Category> listActive();
}
