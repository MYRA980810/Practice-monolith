package com.livecomerce.order;

import java.util.UUID;

public record OrderItemReleasedEvent(UUID orderId, UUID itemId, UUID productId, int quantity) {}
