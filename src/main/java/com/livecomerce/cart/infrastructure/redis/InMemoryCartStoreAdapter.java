package com.livecomerce.cart.infrastructure.redis;

import com.livecomerce.cart.application.port.out.CartStorePort;
import com.livecomerce.cart.domain.Cart;
import com.livecomerce.cart.domain.CartItem;
import com.livecomerce.cart.domain.CartLineKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory mirror of {@link RedisCartStoreAdapter}'s behavioural contract,
 * used under the {@code local} profile (H2/no external Redis) — same shape
 * as {@code InMemoryViewerCountAdapter}.
 */
@Component
@Profile("local")
class InMemoryCartStoreAdapter implements CartStorePort {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCartStoreAdapter.class);

    private record LineKey(UUID buyerId, UUID storeId, UUID productId, UUID variantId) {}

    private final ConcurrentHashMap<LineKey, Integer> lines = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<UUID>> buyerStoreIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<LineKey, Integer> notifiedThresholds = new ConcurrentHashMap<>();

    @Override
    public void addOrIncrement(UUID buyerId, UUID storeId, UUID productId, UUID variantId, int quantity) {
        var key = new LineKey(buyerId, storeId, productId, variantId);
        int resulting = lines.merge(key, quantity, Integer::sum);

        if (resulting > CartItem.MAX_QUANTITY) {
            // JD-R2-002/JDB2-006: the same check-then-act race as JDB2-001,
            // but via addToCart's [1,99] guard instead of changeQuantity's
            // stock guard — two concurrent addToCart calls can each read a
            // pre-race quantity under the cap and jointly push the raw
            // merge above it, bricking the cart on the next load()
            // (CartItem.of() throws for quantity > MAX_QUANTITY).
            resulting = correctOvershoot(key, buyerId, storeId, resulting, CartItem.MAX_QUANTITY);
            if (resulting <= 0) {
                // a concurrent removeLine raced the correction down to zero
                // or below — the line is already removed (see
                // correctOvershoot), don't resurrect it via the index below.
                return;
            }
        }

        index(buyerId, storeId);
    }

    @Override
    public int changeQuantity(UUID buyerId, UUID storeId, UUID productId, UUID variantId, int delta,
            Integer availableStock) {
        var key = new LineKey(buyerId, storeId, productId, variantId);
        int resulting = lines.merge(key, delta, Integer::sum);

        if (resulting <= 0) {
            lines.remove(key);
            pruneIfEmpty(buyerId, storeId);
            return 0;
        }

        if (availableStock != null && resulting > availableStock) {
            // JDB2-001: correct an invisible, permanent overshoot from a
            // concurrent check-then-act race immediately after it happens.
            resulting = correctOvershoot(key, buyerId, storeId, resulting, availableStock);
            if (resulting <= 0) {
                // JD-R2-001: the correction itself raced a concurrent
                // removeLine and landed at/below zero — correctOvershoot
                // already removed the line + pruned the index. Return the
                // same "removed" result the resulting<=0 branch above uses,
                // instead of proceeding to re-index below, which would
                // resurrect the line the buyer removed.
                return 0;
            }
        }

        index(buyerId, storeId);
        return resulting;
    }

    /**
     * Corrects an overshoot (a raw atomic merge that landed above {@code
     * bound}) via a RELATIVE corrective merge — never an absolute {@code
     * put} (JD-R2-001). Because the correction is itself atomic relative to
     * whatever the key's CURRENT value is at the moment it executes, it
     * composes correctly with a concurrent {@code removeLine}: {@link
     * java.util.concurrent.ConcurrentHashMap#merge} on an absent key simply
     * associates it with the (negative) correction delta rather than
     * invoking the remapping function, so the result lands at/below zero and
     * this method removes the line + prunes the index — instead of
     * resurrecting the line an absolute {@code put} would have.
     *
     * <p>Accepted residual (design D5, out of proportion to eliminate):
     * if the line still exists and this correction converges it back to
     * exactly {@code bound}, the caller's subsequent index update can still
     * race very narrowly with a concurrent {@code removeLine}'s {@code
     * pruneIfEmpty}. This self-heals via {@link #loadStoreIds}.
     *
     * @return the corrected quantity, or a value {@code <= 0} if the line
     * was (or became, due to the race above) removed — callers must not
     * proceed to re-index the store in that case.
     */
    private int correctOvershoot(LineKey key, UUID buyerId, UUID storeId, int resulting, int bound) {
        log.warn("overshoot detected for buyer {} store {} product {} variant {}: {} > bound {} — "
                        + "applying corrective decrement",
                buyerId, storeId, key.productId(), key.variantId(), resulting, bound);
        int correctionDelta = bound - resulting;
        int corrected = lines.merge(key, correctionDelta, Integer::sum);
        if (corrected <= 0) {
            lines.remove(key);
            pruneIfEmpty(buyerId, storeId);
            return 0;
        }
        return corrected;
    }

    @Override
    public void removeLine(UUID buyerId, UUID storeId, UUID productId, UUID variantId) {
        lines.remove(new LineKey(buyerId, storeId, productId, variantId));
        pruneIfEmpty(buyerId, storeId);
    }

    private void pruneIfEmpty(UUID buyerId, UUID storeId) {
        boolean stillHasLines = lines.keySet().stream()
                .anyMatch(k -> k.buyerId().equals(buyerId) && k.storeId().equals(storeId));
        if (!stillHasLines) {
            var storeIds = buyerStoreIndex.get(buyerId);
            if (storeIds != null) {
                storeIds.remove(storeId);
            }
        }
    }

    private void index(UUID buyerId, UUID storeId) {
        buyerStoreIndex.computeIfAbsent(buyerId, k -> ConcurrentHashMap.newKeySet()).add(storeId);
    }

    @Override
    public Cart load(UUID buyerId, UUID storeId) {
        List<CartItem> items = lines.entrySet().stream()
                .filter(e -> e.getKey().buyerId().equals(buyerId) && e.getKey().storeId().equals(storeId))
                .map(e -> CartItem.of(new CartLineKey(e.getKey().productId(), e.getKey().variantId()), e.getValue()))
                .toList();
        return Cart.of(buyerId, storeId, items);
    }

    @Override
    public Set<UUID> loadStoreIds(UUID buyerId) {
        var storeIds = buyerStoreIndex.get(buyerId);
        return storeIds == null ? Set.of() : Set.copyOf(storeIds);
    }

    @Override
    public void clear(UUID buyerId, UUID storeId) {
        lines.keySet().removeIf(k -> k.buyerId().equals(buyerId) && k.storeId().equals(storeId));
        var storeIds = buyerStoreIndex.get(buyerId);
        if (storeIds != null) {
            storeIds.remove(storeId);
        }
    }

    @Override
    public Set<UUID> scanAllBuyerIds() {
        return Set.copyOf(buyerStoreIndex.keySet());
    }

    @Override
    public Integer loadNotifiedThreshold(UUID buyerId, UUID storeId, UUID productId, UUID variantId) {
        return notifiedThresholds.get(new LineKey(buyerId, storeId, productId, variantId));
    }

    @Override
    public void recordNotifiedThreshold(UUID buyerId, UUID storeId, UUID productId, UUID variantId, int threshold) {
        notifiedThresholds.put(new LineKey(buyerId, storeId, productId, variantId), threshold);
    }

    @Override
    public void clearNotifiedThreshold(UUID buyerId, UUID storeId, UUID productId, UUID variantId) {
        notifiedThresholds.remove(new LineKey(buyerId, storeId, productId, variantId));
    }
}
