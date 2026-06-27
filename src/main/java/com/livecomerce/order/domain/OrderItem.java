package com.livecomerce.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "item_type", nullable = false, length = 20)
    private OrderItemType itemType;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, length = 10)
    private OrderItemStatus status;

    @Column(name = "reserved_until")
    private OffsetDateTime reservedUntil;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    static OrderItem reserve(Order order, UUID productId, UUID variantId, String productName,
                             BigDecimal unitPrice, String currency, int qty, int ttlMinutes,
                             OrderItemType itemType) {
        var item = new OrderItem();
        item.id            = UUID.randomUUID();
        item.order         = order;
        item.productId     = productId;
        item.variantId     = variantId;
        item.itemType      = itemType;
        item.productName   = productName;
        item.unitPrice     = unitPrice;
        item.currency      = currency;
        item.quantity      = qty;
        item.subtotal      = unitPrice.multiply(BigDecimal.valueOf(qty));
        item.status        = OrderItemStatus.RESERVED;
        item.reservedUntil = OffsetDateTime.now().plusMinutes(ttlMinutes);
        item.createdAt     = OffsetDateTime.now();
        return item;
    }

    void confirmPayment() {
        if (this.status != OrderItemStatus.RESERVED) {
            throw new IllegalStateException(
                    "Cannot confirm payment for item with status: " + this.status);
        }
        this.status        = OrderItemStatus.PAID;
        this.reservedUntil = null;
        this.paidAt        = OffsetDateTime.now();
    }

    void expire() {
        if (this.status == OrderItemStatus.RESERVED) {
            this.status        = OrderItemStatus.EXPIRED;
            this.reservedUntil = null;
        }
    }

    public boolean isExpiredAt(OffsetDateTime now) {
        return this.status == OrderItemStatus.RESERVED
                && this.reservedUntil != null
                && this.reservedUntil.isBefore(now);
    }
}
