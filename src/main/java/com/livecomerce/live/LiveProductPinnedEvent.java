package com.livecomerce.live;

import java.math.BigDecimal;
import java.util.UUID;

public record LiveProductPinnedEvent(
        UUID liveId,
        UUID liveProductId,
        String productName,
        BigDecimal price
) {}
