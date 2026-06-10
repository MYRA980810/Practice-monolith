package com.livecomerce.catalog.application.port.out;

import com.livecomerce.catalog.domain.Category;

public interface SaveCategoryPort {
    Category save(Category category);
}
