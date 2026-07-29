package com.livecomerce.live.api;

import com.livecomerce.live.domain.Live;
import jakarta.annotation.Nullable;

/**
 * Broadcast credentials (Agora RTC token or IVS ingest endpoint/stream key) are only
 * ever returned here, from the seller- and ownership-checked POST /start response —
 * never from LiveResponse, which also backs the public GET/list endpoints.
 */
public record LiveBroadcastResponse(
        LiveResponse live,
        @Nullable String streamToken,
        @Nullable String ivsIngestEndpoint,
        @Nullable String ivsStreamKeyValue
) {
    public static LiveBroadcastResponse from(Live live) {
        return new LiveBroadcastResponse(
                LiveResponse.from(live),
                live.getStreamToken(),
                live.getIvsIngestEndpoint(),
                live.getIvsStreamKeyValue()
        );
    }
}
