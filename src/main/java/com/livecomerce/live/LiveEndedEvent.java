package com.livecomerce.live;

import java.util.UUID;

public record LiveEndedEvent(UUID liveId, UUID sellerId) {}
