package com.livecomerce.live.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AgoraSignalingEvent(
        String channel,
        @JsonProperty("userId") String userId,
        String message,
        String messageId,
        long sendTs,
        String eventType
) {}
