package com.livecomerce.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "buyer_stripe_customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuyerStripeCustomer implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "stripe_customer_id", nullable = false, length = 255)
    private String stripeCustomerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

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

    public static BuyerStripeCustomer create(UUID userId, String stripeCustomerId) {
        var customer = new BuyerStripeCustomer();
        customer.id               = UUID.randomUUID();
        customer.isNew            = true;
        customer.userId           = userId;
        customer.stripeCustomerId = stripeCustomerId;
        customer.createdAt        = OffsetDateTime.now();
        return customer;
    }
}
