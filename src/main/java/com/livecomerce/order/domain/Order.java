package com.livecomerce.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "live_id")
    private UUID liveSessionId;

    @Column(nullable = false, length = 15)
    private OrderStatus status;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "shipping_address_snapshot", columnDefinition = "jsonb")
    private String shippingAddress;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Transient
    private boolean isNew = false;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public static Order open(UUID buyerId, UUID storeId, UUID liveSessionId, String currency) {
        var order = new Order();
        order.id            = UUID.randomUUID();
        order.isNew         = true;
        order.buyerId       = buyerId;
        order.storeId       = storeId;
        order.liveSessionId = liveSessionId;
        order.status        = OrderStatus.OPEN;
        order.currency      = currency != null ? currency : "MXN";
        order.createdAt     = OffsetDateTime.now();
        order.updatedAt     = OffsetDateTime.now();
        return order;
    }

    public OrderItem addItem(UUID productId, UUID variantId, String productName,
                             java.math.BigDecimal unitPrice, String currency,
                             int qty, int ttlMinutes) {
        var item = OrderItem.reserve(this, productId, variantId, productName, unitPrice, currency, qty, ttlMinutes);
        this.items.add(item);
        this.updatedAt = OffsetDateTime.now();
        return item;
    }

    public void confirmItemPayment(UUID itemId) {
        var item = findItem(itemId);
        item.confirmPayment();
        this.updatedAt = OffsetDateTime.now();
    }

    public List<OrderItem> expireReservedItems(OffsetDateTime now) {
        var expired = items.stream()
                .filter(i -> i.isExpiredAt(now))
                .toList();
        expired.forEach(OrderItem::expire);
        this.updatedAt = OffsetDateTime.now();
        return expired;
    }

    public List<OrderItem> finalizeWith(String shippingAddress) {
        var expired = expireReservedItems(OffsetDateTime.now());
        boolean hasPaidItems = items.stream()
                .anyMatch(i -> i.getStatus() == OrderItemStatus.PAID);
        this.status           = hasPaidItems ? OrderStatus.PAID : OrderStatus.CANCELLED;
        this.shippingAddress  = shippingAddress;
        this.updatedAt        = OffsetDateTime.now();
        return expired;
    }

    public void markShipped(String trackingNumber) {
        if (this.status != OrderStatus.PAID) {
            throw new IllegalStateException("Cannot ship an order with status: " + this.status);
        }
        this.status         = OrderStatus.SHIPPED;
        this.trackingNumber = trackingNumber;
        this.updatedAt      = OffsetDateTime.now();
    }

    public void markDelivered() {
        if (this.status != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot deliver an order with status: " + this.status);
        }
        this.status    = OrderStatus.DELIVERED;
        this.updatedAt = OffsetDateTime.now();
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    private OrderItem findItem(UUID itemId) {
        return items.stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
    }
}
