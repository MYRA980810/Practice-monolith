package com.livecomerce.live;

import java.util.List;
import java.util.UUID;

public record LiveStartedEvent(UUID liveId, UUID storeId, String title, List<UUID> subscriberIds) {}
