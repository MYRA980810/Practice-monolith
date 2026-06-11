package com.livecomerce.order;

import java.util.UUID;

public record OrderItemPaidEvent(UUID orderId, UUID itemId, UUID productId, UUID variantId, int quantity) {}
