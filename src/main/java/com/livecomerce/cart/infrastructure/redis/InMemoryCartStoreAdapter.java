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
        lines.merge(key, quantity, Integer::sum);
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
            log.warn("changeQuantity overshoot detected for buyer {} store {} product {} variant {}: "
                            + "{} > availableStock {} — clamping",
                    buyerId, storeId, productId, variantId, resulting, availableStock);
            lines.put(key, availableStock);
            resulting = availableStock;
        }

        index(buyerId, storeId);
        return resulting;
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
