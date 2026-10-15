package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.RefreshTokenStorePort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.domain.RefreshToken;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import com.livecomerce.auth.infrastructure.security.JwtProperties;
import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshAccessTokenServiceTest {

    @Mock LoadUserPort loadUserPort;
    @Mock RefreshTokenStorePort refreshTokenStorePort;
    @Mock TokenGeneratorPort tokenGeneratorPort;
    @Mock RefreshTokenHasher refreshTokenHasher;

    RefreshAccessTokenService service;

    private static final UUID USER_ID   = UUID.randomUUID();
    private static final UUID FAMILY_ID = UUID.randomUUID();
    private static final String RAW_TOKEN = "raw-refresh-token";
    private static final String TOKEN_HASH = "hashed-token";
    private static final long TTL_MS = 30L * 24 * 60 * 60 * 1000;
    private static final long GRACE_MS = 30_000L;

    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        var jwtProperties = new JwtProperties("test-secret", 900_000L, TTL_MS, GRACE_MS);
        service = new RefreshAccessTokenService(refreshTokenStorePort, loadUserPort, tokenGeneratorPort,
                refreshTokenHasher, jwtProperties, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void refresh_withValidToken_rotatesAndReturnsNewAccessToken() {
        var oldToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, 30L * 24 * 60 * 60 * 1000);
        var user = User.create("user@test.com", "hash", "John", "Doe", null, Role.SELLER);
        user.verify();

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(oldToken));
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));
        when(refreshTokenStorePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenGeneratorPort.generate(user)).thenReturn("new-access-token");
        when(refreshTokenHasher.generateRawToken()).thenReturn("new-raw-token");
        when(refreshTokenHasher.hash("new-raw-token")).thenReturn("new-hash");

        var result = service.refresh(RAW_TOKEN);

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-raw-token");

        // old token should be marked used+revoked
        assertThat(oldToken.isRevoked()).isTrue();
        assertThat(oldToken.getUsedAt()).isNotNull();
        assertThat(oldToken.getReplacedBy()).isNotNull();
    }

    @Test
    void refresh_whenTokenNotFound_throwsRefreshTokenInvalidException() {
        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class);
    }

    @Test
    void refresh_whenTokenExpired_throwsRefreshTokenInvalidException() {
        var expiredToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, -1000L);

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class);
    }

    @Test
    void refresh_whenTokenRevoked_throwsRefreshTokenInvalidException() {
        var revokedToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, 30L * 24 * 60 * 60 * 1000);
        revokedToken.revoke();

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class);
    }

    @Test
    void refresh_whenTokenAlreadyUsed_revokesFamilyAndThrowsRefreshReuseException() {
        var usedToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, 30L * 24 * 60 * 60 * 1000);
        usedToken.markUsed(); // already consumed

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenReuseException.class);

        verify(refreshTokenStorePort).revokeFamily(FAMILY_ID);
    }

    @Test
    void refresh_whenReusedWithinGraceAndSuccessorActive_throwsRetryWithoutRevokingOrSaving() {
        var successor = RefreshToken.issue(USER_ID, FAMILY_ID, "successor-hash", TTL_MS);
        var usedToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, TTL_MS);
        usedToken.rotate(successor.getId(), now.minusMillis(5_000));

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(usedToken));
        when(refreshTokenStorePort.loadById(successor.getId())).thenReturn(Optional.of(successor));

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenRetryException.class);

        verify(refreshTokenStorePort, never()).revokeFamily(any());
        verify(refreshTokenStorePort, never()).save(any());
        verify(tokenGeneratorPort, never()).generate(any());
    }

    @Test
    void refresh_whenReusedExactlyAtGraceBoundary_throwsRetry() {
        var successor = RefreshToken.issue(USER_ID, FAMILY_ID, "successor-hash", TTL_MS);
        var usedToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, TTL_MS);
        usedToken.rotate(successor.getId(), now.minusMillis(GRACE_MS));

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(usedToken));
        when(refreshTokenStorePort.loadById(successor.getId())).thenReturn(Optional.of(successor));

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenRetryException.class);

        verify(refreshTokenStorePort, never()).revokeFamily(any());
    }

    @Test
    void refresh_whenReusedAfterGraceWindow_revokesFamilyAndThrowsRefreshReuseException() {
        var successor = RefreshToken.issue(USER_ID, FAMILY_ID, "successor-hash", TTL_MS);
        var usedToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, TTL_MS);
        usedToken.rotate(successor.getId(), now.minusMillis(GRACE_MS + 1));

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenReuseException.class);

        verify(refreshTokenStorePort).revokeFamily(FAMILY_ID);
    }

    @Test
    void refresh_whenReusedWithinGraceButSuccessorRevoked_revokesFamilyAndThrowsRefreshReuseException() {
        var successor = RefreshToken.issue(USER_ID, FAMILY_ID, "successor-hash", TTL_MS);
        successor.revoke(); // e.g. logout or password change revoked the family
        var usedToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, TTL_MS);
        usedToken.rotate(successor.getId(), now.minusMillis(5_000));

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(usedToken));
        when(refreshTokenStorePort.loadById(successor.getId())).thenReturn(Optional.of(successor));

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenReuseException.class);

        verify(refreshTokenStorePort).revokeFamily(FAMILY_ID);
    }

    @Test
    void refresh_whenReusedWithinGraceButSuccessorMissing_revokesFamilyAndThrowsRefreshReuseException() {
        var successorId = UUID.randomUUID();
        var usedToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, TTL_MS);
        usedToken.rotate(successorId, now.minusMillis(5_000));

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(usedToken));
        when(refreshTokenStorePort.loadById(successorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenReuseException.class);

        verify(refreshTokenStorePort).revokeFamily(FAMILY_ID);
    }

    @Test
    void refresh_whenRowLockCannotBeAcquired_throwsRetryWithoutRevokingOrSaving() {
        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHashForUpdate(TOKEN_HASH))
                .thenThrow(new CannotAcquireLockException("lock timeout"));

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenRetryException.class)
                .hasCauseInstanceOf(CannotAcquireLockException.class);

        verify(refreshTokenStorePort, never()).revokeFamily(any());
        verify(refreshTokenStorePort, never()).save(any());
        verify(tokenGeneratorPort, never()).generate(any());
    }

    @Test
    void refresh_whenLoadFailsForOtherReason_propagatesOriginalException() {
        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHashForUpdate(TOKEN_HASH))
                .thenThrow(new DataAccessResourceFailureException("connection lost"));

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void refresh_withValidToken_recordsUsedAtFromClock() {
        var oldToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, TTL_MS);
        var user = User.create("user@test.com", "hash", "John", "Doe", null, Role.SELLER);

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(oldToken));
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));
        when(refreshTokenStorePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenGeneratorPort.generate(user)).thenReturn("new-access-token");
        when(refreshTokenHasher.generateRawToken()).thenReturn("new-raw-token");
        when(refreshTokenHasher.hash("new-raw-token")).thenReturn("new-hash");

        service.refresh(RAW_TOKEN);

        assertThat(oldToken.getUsedAt()).isEqualTo(now);
    }

    @Test
    void refresh_whenUserNoLongerExists_revokesFamilyAndThrowsInvalidWithoutIssuing() {
        var oldToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, TTL_MS);

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(oldToken));
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class);

        verify(refreshTokenStorePort).revokeFamily(FAMILY_ID);
        verify(refreshTokenStorePort, never()).save(any());
        verify(tokenGeneratorPort, never()).generate(any());
    }

    @Test
    void refresh_whenUserIsInactive_revokesFamilyAndThrowsInvalidWithoutIssuing() {
        var oldToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, TTL_MS);
        var user = User.create("user@test.com", "hash", "John", "Doe", null, Role.SELLER);
        user.deactivate();

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(oldToken));
        when(loadUserPort.loadById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class);

        verify(refreshTokenStorePort).revokeFamily(FAMILY_ID);
        verify(refreshTokenStorePort, never()).save(any());
        verify(tokenGeneratorPort, never()).generate(any());
    }

    /** The family revocation for a missing/inactive user must commit while the 401 propagates. */
    @Test
    void refresh_doesNotRollBackOnInvalidOrReuse() throws Exception {
        var tx = RefreshAccessTokenService.class.getMethod("refresh", String.class)
                .getAnnotation(Transactional.class);

        assertThat(tx.noRollbackFor())
                .contains(RefreshTokenReuseException.class, RefreshTokenInvalidException.class);
    }
}
