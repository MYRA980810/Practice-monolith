package com.livecomerce.live.api;

import com.livecomerce.live.domain.LiveProduct;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

public record LiveProductResponse(
        UUID id,
        UUID liveId,
        @Nullable UUID productId,
        @Nullable UUID variantId,
        String productNameSnapshot,
        BigDecimal priceSnapshot,
        String currencySnapshot,
        int stockAllocated,
        int stockSold,
        boolean isHot,
        boolean isPinned,
        int position
) {
    public static LiveProductResponse from(LiveProduct lp) {
        return new LiveProductResponse(
                lp.getId(),
                lp.getLive().getId(),
                lp.getProductId(),
                lp.getVariantId(),
                lp.getProductNameSnapshot(),
                lp.getPriceSnapshot(),
                lp.getCurrencySnapshot(),
                lp.getStockAllocated(),
                lp.getStockSold(),
                lp.isHot(),
                lp.isPinned(),
                lp.getPosition()
        );
    }
}
