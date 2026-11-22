package com.livecomerce.live.infrastructure.ivs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Body the IVS Chat message review Lambda ({@code infra/ivs-chat-message-review}) sends to
 * {@code ivs-chat-messages} for every allowed message. Kept as raw strings so the listener
 * validates and reports each field itself.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record IvsChatMessagePayload(
        String messageId,
        String liveId,
        String userId,
        String displayName,
        String content,
        String sentAt
) {}
