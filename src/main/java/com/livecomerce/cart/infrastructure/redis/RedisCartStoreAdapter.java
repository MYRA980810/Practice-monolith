package com.livecomerce.cart.infrastructure.redis;

import com.livecomerce.cart.application.port.out.CartStorePort;
import com.livecomerce.cart.domain.Cart;
import com.livecomerce.cart.domain.CartItem;
import com.livecomerce.cart.domain.CartLineKey;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Redis hash-per-store-cart + set-index adapter (design D1/D5).
 *
 * <p>Key shapes: {@code cart:{buyerId}:{storeId}} (Hash, field {@code
 * "{productId}:{variantId|none}"}, value = qty) and {@code
 * buyer:{buyerId}:cart-stores} (Set of storeId strings). Both keys get a 7
 * day TTL refreshed on every write (D5): hash write → index {@code SADD} →
 * {@code EXPIRE} both, in that order.
 */
@Component
@Profile("!local")
@RequiredArgsConstructor
class RedisCartStoreAdapter implements CartStorePort {

    private static final Logger log = LoggerFactory.getLogger(RedisCartStoreAdapter.class);
    private static final Duration CART_TTL = Duration.ofDays(7);
    private static final String NO_VARIANT = "none";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void addOrIncrement(UUID buyerId, UUID storeId, UUID productId, UUID variantId, int quantity) {
        String cartKey = cartKey(buyerId, storeId);
        String indexKey = indexKey(buyerId);

        redisTemplate.opsForHash().increment(cartKey, field(productId, variantId), quantity);
        redisTemplate.opsForSet().add(indexKey, storeId.toString());
        redisTemplate.expire(cartKey, CART_TTL);
        redisTemplate.expire(indexKey, CART_TTL);
    }

    @Override
    public int changeQuantity(UUID buyerId, UUID storeId, UUID productId, UUID variantId, int delta,
            Integer availableStock) {
        String cartKey = cartKey(buyerId, storeId);
        String field = field(productId, variantId);

        Long result = redisTemplate.opsForHash().increment(cartKey, field, delta);
        long resulting = result == null ? 0L : result;

        if (resulting <= 0) {
            redisTemplate.opsForHash().delete(cartKey, field);
            pruneIfEmpty(buyerId, storeId);
            return 0;
        }

        if (availableStock != null && resulting > availableStock) {
            // JDB2-001: a concurrent check-then-act race can push the raw
            // atomic increment above availableStock even though each caller
            // validated against it individually. Correct the persisted
            // state immediately rather than leaving an invisible,
            // permanent overshoot.
            log.warn("changeQuantity overshoot detected for cart {} field {}: {} > availableStock {} — clamping",
                    cartKey, field, resulting, availableStock);
            redisTemplate.opsForHash().put(cartKey, field, String.valueOf(availableStock));
            resulting = availableStock;
        }

        String indexKey = indexKey(buyerId);
        redisTemplate.opsForSet().add(indexKey, storeId.toString());
        redisTemplate.expire(cartKey, CART_TTL);
        redisTemplate.expire(indexKey, CART_TTL);
        return (int) resulting;
    }

    @Override
    public void removeLine(UUID buyerId, UUID storeId, UUID productId, UUID variantId) {
        String cartKey = cartKey(buyerId, storeId);
        redisTemplate.opsForHash().delete(cartKey, field(productId, variantId));
        pruneIfEmpty(buyerId, storeId);
    }

    private void pruneIfEmpty(UUID buyerId, UUID storeId) {
        String cartKey = cartKey(buyerId, storeId);
        Long remaining = redisTemplate.opsForHash().size(cartKey);
        if (remaining == null || remaining <= 0) {
            redisTemplate.delete(cartKey);
            redisTemplate.opsForSet().remove(indexKey(buyerId), storeId.toString());
        }
    }

    @Override
    public Cart load(UUID buyerId, UUID storeId) {
        var entries = redisTemplate.opsForHash().entries(cartKey(buyerId, storeId));
        if (entries.isEmpty()) {
            return Cart.empty(buyerId, storeId);
        }

        List<CartItem> items = new ArrayList<>();
        for (var entry : entries.entrySet()) {
            String fieldKey = String.valueOf(entry.getKey());
            int quantity = Integer.parseInt(String.valueOf(entry.getValue()));
            items.add(CartItem.of(parseKey(fieldKey), quantity));
        }
        return Cart.of(buyerId, storeId, items);
    }

    @Override
    public Set<UUID> loadStoreIds(UUID buyerId) {
        String indexKey = indexKey(buyerId);
        Set<String> members = redisTemplate.opsForSet().members(indexKey);
        if (members == null || members.isEmpty()) {
            return Set.of();
        }

        Set<UUID> valid = new java.util.LinkedHashSet<>();
        for (String member : members) {
            UUID storeId = UUID.fromString(member);
            Long size = redisTemplate.opsForHash().size(cartKey(buyerId, storeId));
            if (size != null && size > 0) {
                valid.add(storeId);
            } else {
                redisTemplate.opsForSet().remove(indexKey, member);
            }
        }
        return valid;
    }

    @Override
    public void clear(UUID buyerId, UUID storeId) {
        redisTemplate.delete(cartKey(buyerId, storeId));
        redisTemplate.opsForSet().remove(indexKey(buyerId), storeId.toString());
    }

    @Override
    public Set<UUID> scanAllBuyerIds() {
        Set<UUID> buyerIds = new java.util.LinkedHashSet<>();
        var scanOptions = org.springframework.data.redis.core.ScanOptions.scanOptions()
                .match("buyer:*:cart-stores").count(200).build();
        try (var cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String[] parts = key.split(":");
                if (parts.length == 3) {
                    buyerIds.add(UUID.fromString(parts[1]));
                }
            }
        }
        return buyerIds;
    }

    @Override
    public Integer loadNotifiedThreshold(UUID buyerId, UUID storeId, UUID productId, UUID variantId) {
        String value = redisTemplate.opsForValue().get(notifiedKey(buyerId, storeId, productId, variantId));
        return value == null ? null : Integer.parseInt(value);
    }

    @Override
    public void recordNotifiedThreshold(UUID buyerId, UUID storeId, UUID productId, UUID variantId, int threshold) {
        redisTemplate.opsForValue().set(
                notifiedKey(buyerId, storeId, productId, variantId), String.valueOf(threshold), CART_TTL);
    }

    @Override
    public void clearNotifiedThreshold(UUID buyerId, UUID storeId, UUID productId, UUID variantId) {
        redisTemplate.delete(notifiedKey(buyerId, storeId, productId, variantId));
    }

    private static String cartKey(UUID buyerId, UUID storeId) {
        return "cart:" + buyerId + ":" + storeId;
    }

    private static String indexKey(UUID buyerId) {
        return "buyer:" + buyerId + ":cart-stores";
    }

    private static String notifiedKey(UUID buyerId, UUID storeId, UUID productId, UUID variantId) {
        return "cart:" + buyerId + ":" + storeId + ":notified:" + productId + ":" + (variantId == null ? NO_VARIANT : variantId);
    }

    private static String field(UUID productId, UUID variantId) {
        return productId + ":" + (variantId == null ? NO_VARIANT : variantId.toString());
    }

    private static CartLineKey parseKey(String field) {
        int idx = field.lastIndexOf(':');
        UUID productId = UUID.fromString(field.substring(0, idx));
        String variantPart = field.substring(idx + 1);
        UUID variantId = NO_VARIANT.equals(variantPart) ? null : UUID.fromString(variantPart);
        return new CartLineKey(productId, variantId);
    }
}
