package com.livecomerce.payment.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SellerConnectAccountTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private SellerConnectAccount buildAccount() {
        return SellerConnectAccount.create(USER_ID, "acct_123456");
    }

    @Test
    void create_setsFieldsCorrectly() {
        var account = buildAccount();

        assertThat(account.getId()).isNotNull();
        assertThat(account.getUserId()).isEqualTo(USER_ID);
        assertThat(account.getStripeAccountId()).isEqualTo("acct_123456");
        assertThat(account.isChargesEnabled()).isFalse();
        assertThat(account.isPayoutsEnabled()).isFalse();
        assertThat(account.isDetailsSubmitted()).isFalse();
        assertThat(account.getActivatedAt()).isNull();
        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateCapabilities_whenNotAllThreeTrue_activatedAtStaysNull() {
        var account = buildAccount();

        account.updateCapabilities(true, true, false);

        assertThat(account.isChargesEnabled()).isTrue();
        assertThat(account.isPayoutsEnabled()).isTrue();
        assertThat(account.isDetailsSubmitted()).isFalse();
        assertThat(account.getActivatedAt()).isNull();
    }

    @Test
    void updateCapabilities_firstTimeAllThreeTrue_setsActivatedAt() {
        var account = buildAccount();

        account.updateCapabilities(true, true, true);

        assertThat(account.isChargesEnabled()).isTrue();
        assertThat(account.isPayoutsEnabled()).isTrue();
        assertThat(account.isDetailsSubmitted()).isTrue();
        assertThat(account.getActivatedAt()).isNotNull();
    }

    @Test
    void updateCapabilities_whenAlreadyActivated_activatedAtDoesNotChange() {
        var account = buildAccount();

        account.updateCapabilities(true, true, true);
        var firstActivatedAt = account.getActivatedAt();
        assertThat(firstActivatedAt).isNotNull();

        account.updateCapabilities(true, true, true);
        var secondActivatedAt = account.getActivatedAt();

        assertThat(secondActivatedAt).isEqualTo(firstActivatedAt);
    }
}
