package com.livecomerce.live.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public record AgoraWebhookEvent(
        String noticeId,
        @JsonProperty("productId") int productCategory,
        int eventType,
        @Nullable String clientSeq,
        @Nullable AgoraPayload payload
) {
    public record AgoraPayload(
            @Nullable String cname,
            @Nullable String uid,
            @Nullable String sid,
            @Nullable Integer sequence,
            @Nullable Integer sendts,
            @Nullable Integer serviceType,
            @Nullable AgoraDetails details
    ) {}

    public record AgoraDetails(
            @Nullable Integer msgName,
            @Nullable Integer status
    ) {}
}
