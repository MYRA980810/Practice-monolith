package com.livecomerce.live.api;

import com.livecomerce.live.application.port.out.LoadLivePort.CategoryLiveCount;

import java.util.UUID;

public record LiveCategoryCountResponse(UUID categoryId, long count) {

    public static LiveCategoryCountResponse from(CategoryLiveCount categoryCount) {
        return new LiveCategoryCountResponse(categoryCount.categoryId(), categoryCount.count());
    }
}
