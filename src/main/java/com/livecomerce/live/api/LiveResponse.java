package com.livecomerce.live.api;

import com.livecomerce.live.domain.Live;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LiveResponse(
        UUID id,
        UUID sellerId,
        @Nullable UUID storeId,
        String liveContext,
        String title,
        String status,
        @Nullable String agoraChannelId,
        @Nullable String streamToken,
        @Nullable String thumbnailUrl,
        @Nullable Instant scheduledAt,
        @Nullable Instant startedAt,
        @Nullable Instant endedAt,
        int peakViewers,
        int displayDurationSeconds,
        OffsetDateTime createdAt,
        @Nullable String ivsPlaybackUrl
) {
    public static LiveResponse from(Live live) {
        return new LiveResponse(
                live.getId(),
                live.getSellerId(),
                live.getStoreId(),
                live.getContext().getCode(),
                live.getTitle(),
                live.getStatus().getCode(),
                live.getAgoraChannelId(),
                live.getStreamToken(),
                live.getThumbnailUrl(),
                live.getScheduledAt(),
                live.getStartedAt(),
                live.getEndedAt(),
                live.getPeakViewers(),
                live.getDisplayDurationSeconds(),
                live.getCreatedAt(),
                live.getIvsPlaybackUrl()
        );
    }
}
