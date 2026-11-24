package com.livecomerce.live.api;

import com.livecomerce.live.domain.Live;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * @param chatProvider {@code "ivs"} when the live has an IVS Chat room, otherwise {@code "agora"}.
 *                     Decided per live from its own state (never the global {@code live.chat-provider}
 *                     flag), so a live started on Agora stays on Agora after the flag flips.
 */
public record LiveResponse(
        UUID id,
        UUID sellerId,
        @Nullable UUID storeId,
        String liveContext,
        String title,
        String status,
        @Nullable String agoraChannelId,
        @Nullable String thumbnailUrl,
        @Nullable Instant scheduledAt,
        @Nullable Instant startedAt,
        @Nullable Instant endedAt,
        int peakViewers,
        int displayDurationSeconds,
        OffsetDateTime createdAt,
        @Nullable String ivsPlaybackUrl,
        @Nullable UUID categoryId,
        String chatProvider
) {
    static final String CHAT_PROVIDER_IVS   = "ivs";
    static final String CHAT_PROVIDER_AGORA = "agora";

    public static LiveResponse from(Live live) {
        return new LiveResponse(
                live.getId(),
                live.getSellerId(),
                live.getStoreId(),
                live.getContext().getCode(),
                live.getTitle(),
                live.getStatus().getCode(),
                live.getAgoraChannelId(),
                live.getThumbnailUrl(),
                live.getScheduledAt(),
                live.getStartedAt(),
                live.getEndedAt(),
                live.getPeakViewers(),
                live.getDisplayDurationSeconds(),
                live.getCreatedAt(),
                live.getIvsPlaybackUrl(),
                live.getCategoryId(),
                live.hasIvsChatRoom() ? CHAT_PROVIDER_IVS : CHAT_PROVIDER_AGORA
        );
    }
}
