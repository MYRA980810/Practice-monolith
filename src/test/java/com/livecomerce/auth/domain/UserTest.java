package com.livecomerce.auth.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private User buildUser() {
        return User.create("user@test.com", "hashed-password", "John", "Doe", null, Role.SELLER);
    }

    @Test
    void create_isNotProfileCompleteByDefault() {
        var user = buildUser();
        assertThat(user.isProfileComplete()).isFalse();
    }

    @Test
    void markAddressRequirementMet_alone_doesNotCompleteProfile() {
        var user = buildUser();

        user.markAddressRequirementMet();

        assertThat(user.isAddressRequirementMet()).isTrue();
        assertThat(user.isPaymentRequirementMet()).isFalse();
        assertThat(user.isProfileComplete()).isFalse();
    }

    @Test
    void markPaymentRequirementMet_alone_doesNotCompleteProfile() {
        var user = buildUser();

        user.markPaymentRequirementMet();

        assertThat(user.isPaymentRequirementMet()).isTrue();
        assertThat(user.isAddressRequirementMet()).isFalse();
        assertThat(user.isProfileComplete()).isFalse();
    }

    @Test
    void markBothRequirementsMet_addressThenPayment_completesProfile() {
        var user = buildUser();

        user.markAddressRequirementMet();
        user.markPaymentRequirementMet();

        assertThat(user.isAddressRequirementMet()).isTrue();
        assertThat(user.isPaymentRequirementMet()).isTrue();
        assertThat(user.isProfileComplete()).isTrue();
    }

    @Test
    void markBothRequirementsMet_paymentThenAddress_completesProfile() {
        var user = buildUser();

        user.markPaymentRequirementMet();
        user.markAddressRequirementMet();

        assertThat(user.isAddressRequirementMet()).isTrue();
        assertThat(user.isPaymentRequirementMet()).isTrue();
        assertThat(user.isProfileComplete()).isTrue();
    }

    @Test
    void markRequirementMetAgain_afterProfileComplete_isIdempotent() {
        var user = buildUser();
        user.markAddressRequirementMet();
        user.markPaymentRequirementMet();
        assertThat(user.isProfileComplete()).isTrue();

        user.markAddressRequirementMet();
        user.markPaymentRequirementMet();

        assertThat(user.isProfileComplete()).isTrue();
    }
}
