package com.livecomerce.live.infrastructure.ivs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * EventBridge envelope for Amazon IVS "IVS Stream State Change" events, delivered via SQS.
 * Only the fields this slice needs are modeled; the real payload carries additional
 * top-level fields (version, id, account, time, region) that are intentionally ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record IvsEventBridgeEvent(
        @JsonProperty("detail-type") @Nullable String detailType,
        @Nullable String source,
        @Nullable List<String> resources,
        @Nullable IvsEventDetail detail) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IvsEventDetail(
            @JsonProperty("event_name") @Nullable String eventName,
            @JsonProperty("channel_name") @Nullable String channelName,
            @JsonProperty("stream_id") @Nullable String streamId,
            @Nullable String code) {}
}
