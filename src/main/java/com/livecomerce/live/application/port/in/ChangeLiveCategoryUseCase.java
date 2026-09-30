package com.livecomerce.live.application.port.in;

import com.livecomerce.live.domain.Live;

import java.util.UUID;

public interface ChangeLiveCategoryUseCase {

    Live changeCategory(ChangeLiveCategoryCommand command);

    record ChangeLiveCategoryCommand(UUID liveId, UUID sellerId, UUID categoryId) {}
}
