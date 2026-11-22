package com.livecomerce.live.api;

import java.time.Instant;

public record IvsChatTokenResponse(String token, Instant sessionExpirationTime,
                                   Instant tokenExpirationTime, String region) {}
