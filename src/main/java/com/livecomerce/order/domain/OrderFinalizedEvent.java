package com.livecomerce.order.domain;

import java.util.UUID;

public record OrderFinalizedEvent(UUID orderId, UUID buyerId, UUID storeId, OrderStatus status) {}
