package com.livecomerce.live.api;

import java.time.Instant;

/** {@code canSend} is false when the user is banned from the seller's chat: they can read but not send. */
public record IvsChatTokenResponse(String token, Instant sessionExpirationTime,
                                   Instant tokenExpirationTime, String region, boolean canSend) {}
