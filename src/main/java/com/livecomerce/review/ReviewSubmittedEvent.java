package com.livecomerce.review;

import java.util.UUID;

public record ReviewSubmittedEvent(UUID orderId, UUID storeId, UUID buyerId) {}
