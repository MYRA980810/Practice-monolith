package com.livecomerce.payment.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BuyerPaymentMethodTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private BuyerPaymentMethod buildMethod() {
        return BuyerPaymentMethod.create(
                USER_ID,
                "pm_123456",
                "visa",
                "4242",
                (short) 12,
                (short) 2030
        );
    }

    @Test
    void create_setsFieldsCorrectly() {
        var method = buildMethod();

        assertThat(method.getId()).isNotNull();
        assertThat(method.getUserId()).isEqualTo(USER_ID);
        assertThat(method.getStripePaymentMethodId()).isEqualTo("pm_123456");
        assertThat(method.getBrand()).isEqualTo("visa");
        assertThat(method.getLast4()).isEqualTo("4242");
        assertThat(method.getExpMonth()).isEqualTo((short) 12);
        assertThat(method.getExpYear()).isEqualTo((short) 2030);
        assertThat(method.isDefault()).isFalse();
        assertThat(method.getCreatedAt()).isNotNull();
    }

    @Test
    void create_isNotDefaultByDefault() {
        var method = buildMethod();
        assertThat(method.isDefault()).isFalse();
    }

    @Test
    void setAsDefault_setsIsDefaultTrue() {
        var method = buildMethod();
        method.setAsDefault();
        assertThat(method.isDefault()).isTrue();
    }

    @Test
    void removeDefault_setsIsDefaultFalse() {
        var method = buildMethod();
        method.setAsDefault();
        assertThat(method.isDefault()).isTrue();

        method.removeDefault();
        assertThat(method.isDefault()).isFalse();
    }
}
