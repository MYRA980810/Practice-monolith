package com.livecomerce.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "buyer_payment_methods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuyerPaymentMethod implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "stripe_payment_method_id", nullable = false, length = 255)
    private String stripePaymentMethodId;

    @Column(nullable = false, length = 30)
    private String brand;

    @Column(nullable = false, length = 4)
    private String last4;

    @Column(name = "exp_month", nullable = false)
    private short expMonth;

    @Column(name = "exp_year", nullable = false)
    private short expYear;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

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

    public static BuyerPaymentMethod create(UUID userId, String stripePaymentMethodId,
            String brand, String last4, short expMonth, short expYear) {
        var method = new BuyerPaymentMethod();
        method.id                     = UUID.randomUUID();
        method.isNew                  = true;
        method.userId                 = userId;
        method.stripePaymentMethodId  = stripePaymentMethodId;
        method.brand                  = brand;
        method.last4                  = last4;
        method.expMonth               = expMonth;
        method.expYear                = expYear;
        method.isDefault              = false;
        method.createdAt              = OffsetDateTime.now();
        return method;
    }

    public void setAsDefault() {
        this.isDefault = true;
    }

    public void removeDefault() {
        this.isDefault = false;
    }
}
