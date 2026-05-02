package com.livecomerce.order.domain;

import java.util.UUID;

public record OrderItemPaidEvent(UUID orderId, UUID itemId, UUID productId, int quantity) {}
