package com.livecomerce.live.application.port.in;

import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public interface CreateLiveUseCase {

    Live createLive(CreateLiveCommand command);

    record CreateLiveCommand(
            UUID sellerId,
            @Nullable UUID storeId,
            LiveContext context,
            String title,
            @Nullable String thumbnailUrl,
            @Nullable Instant scheduledAt,
            int displayDurationSeconds
    ) {}
}
