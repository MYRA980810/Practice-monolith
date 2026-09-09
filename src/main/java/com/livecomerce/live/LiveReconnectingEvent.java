package com.livecomerce.live;

import java.util.UUID;

public record LiveReconnectingEvent(UUID liveId, UUID sellerId, String reason) {}
