package com.livecomerce.live.api;

import com.livecomerce.live.domain.LiveContext;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateLiveRequest(
        @NotNull LiveContext context,
        @Nullable UUID storeId,
        @NotBlank String title,
        @NotBlank String thumbnailUrl,
        @Nullable Instant scheduledAt,
        @Nullable @Min(15) @Max(120) Integer displayDurationSeconds
) {}
