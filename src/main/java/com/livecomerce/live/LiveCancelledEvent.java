package com.livecomerce.live;

import java.util.List;
import java.util.UUID;

public record LiveCancelledEvent(UUID liveId, String title, List<UUID> subscriberIds) {}
