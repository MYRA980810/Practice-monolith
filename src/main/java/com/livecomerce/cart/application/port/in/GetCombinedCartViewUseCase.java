package com.livecomerce.cart.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Returns all of a buyer's active per-store carts in one call, hydrated live
 * from {@code catalog}. A store whose {@code Store} record has been
 * deactivated (all its products deactivated as a result — see {@code
 * catalog.application.StoreEventListener}) is excluded entirely, without
 * touching the underlying Redis data. A stale index entry self-heals by
 * pruning on read (design D5). The live-exclusivity gate does not hide
 * pre-existing lines here — it only flags them (design D4): a line is
 * visible with {@code blockedReason = "LIVE_EXCLUSIVE"} rather than removed.
 */
public interface GetCombinedCartViewUseCase {

    CombinedCartView getCombinedView(UUID buyerId);

    record CombinedCartView(List<StoreCartView> stores) {}

    record StoreCartView(UUID storeId, List<CartLineView> lines) {}

    /**
     * {@code blockedReason} is {@code null} for a purchasable line, or
     * {@code LIVE_EXCLUSIVE} for a line flagged (not hidden) because its
     * product has since become live-exclusive.
     */
    record CartLineView(
            UUID productId,
            UUID variantId,
            String name,
            String imageUrl,
            BigDecimal unitPrice,
            String currency,
            int quantity,
            int availableStock,
            String blockedReason
    ) {}
}
