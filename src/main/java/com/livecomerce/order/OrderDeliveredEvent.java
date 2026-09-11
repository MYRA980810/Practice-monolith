package com.livecomerce.order;

import java.util.UUID;

public record OrderDeliveredEvent(UUID orderId, UUID buyerId, UUID storeId) {}
