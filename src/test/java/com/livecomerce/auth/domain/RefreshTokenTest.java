package com.livecomerce.auth.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenTest {

    private static final UUID USER_ID   = UUID.randomUUID();
    private static final UUID FAMILY_ID = UUID.randomUUID();
    private static final String HASH    = "abc123hash";

    @Test
    void issue_createsActiveNonExpiredToken() {
        var token = RefreshToken.issue(USER_ID, FAMILY_ID, HASH, 30L * 24 * 60 * 60 * 1000);

        assertThat(token.getId()).isNotNull();
        assertThat(token.getUserId()).isEqualTo(USER_ID);
        assertThat(token.getFamilyId()).isEqualTo(FAMILY_ID);
        assertThat(token.getTokenHash()).isEqualTo(HASH);
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.getUsedAt()).isNull();
        assertThat(token.getReplacedBy()).isNull();
        assertThat(token.isExpired(Instant.now())).isFalse();
        assertThat(token.isActive(Instant.now())).isTrue();
    }

    @Test
    void isExpired_whenExpiresAtIsInPast_returnsTrue() {
        var token = RefreshToken.issue(USER_ID, FAMILY_ID, HASH, -1000L); // already expired
        assertThat(token.isExpired(Instant.now())).isTrue();
    }

    @Test
    void isActive_whenRevoked_returnsFalse() {
        var token = RefreshToken.issue(USER_ID, FAMILY_ID, HASH, 30L * 24 * 60 * 60 * 1000);
        token.revoke();
        assertThat(token.isActive(Instant.now())).isFalse();
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void isActive_whenExpired_returnsFalse() {
        var token = RefreshToken.issue(USER_ID, FAMILY_ID, HASH, -1000L);
        assertThat(token.isActive(Instant.now())).isFalse();
    }

    @Test
    void rotate_setsReplacedByAndMarksUsedAndRevoked() {
        var token = RefreshToken.issue(USER_ID, FAMILY_ID, HASH, 30L * 24 * 60 * 60 * 1000);
        var replacementId = UUID.randomUUID();

        token.rotate(replacementId);

        assertThat(token.getReplacedBy()).isEqualTo(replacementId);
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void markUsed_setsUsedAt() {
        var token = RefreshToken.issue(USER_ID, FAMILY_ID, HASH, 30L * 24 * 60 * 60 * 1000);
        assertThat(token.getUsedAt()).isNull();

        token.markUsed();

        assertThat(token.getUsedAt()).isNotNull();
    }

    @Test
    void isConsumed_whenUsedAtSet_returnsTrue() {
        var token = RefreshToken.issue(USER_ID, FAMILY_ID, HASH, 30L * 24 * 60 * 60 * 1000);
        assertThat(token.isConsumed()).isFalse();

        token.markUsed();
        assertThat(token.isConsumed()).isTrue();
    }
}
