package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.RefreshTokenStorePort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.domain.RefreshToken;
import com.livecomerce.auth.domain.Role;
import com.livecomerce.auth.domain.User;
import com.livecomerce.auth.infrastructure.security.JwtProperties;
import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshAccessTokenServiceTest {

    @Mock LoadUserPort loadUserPort;
    @Mock RefreshTokenStorePort refreshTokenStorePort;
    @Mock TokenGeneratorPort tokenGeneratorPort;
    @Mock RefreshTokenHasher refreshTokenHasher;
    @Mock JwtProperties jwtProperties;

    @InjectMocks RefreshAccessTokenService service;

    private static final UUID USER_ID   = UUID.randomUUID();
    private static final UUID FAMILY_ID = UUID.randomUUID();
    private static final String RAW_TOKEN = "raw-refresh-token";
    private static final String TOKEN_HASH = "hashed-token";

    @Test
    void refresh_withValidToken_rotatesAndReturnsNewAccessToken() {
        var oldToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, 30L * 24 * 60 * 60 * 1000);
        var user = User.create("user@test.com", "hash", "John", "Doe", null, Role.SELLER);
        user.verify();

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(oldToken));
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
        when(refreshTokenStorePort.loadByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class);
    }

    @Test
    void refresh_whenTokenExpired_throwsRefreshTokenInvalidException() {
        var expiredToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, -1000L);

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class);
    }

    @Test
    void refresh_whenTokenRevoked_throwsRefreshTokenInvalidException() {
        var revokedToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, 30L * 24 * 60 * 60 * 1000);
        revokedToken.revoke();

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class);
    }

    @Test
    void refresh_whenTokenAlreadyUsed_revokesFamilyAndThrowsRefreshReuseException() {
        var usedToken = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, 30L * 24 * 60 * 60 * 1000);
        usedToken.markUsed(); // already consumed

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() -> service.refresh(RAW_TOKEN))
                .isInstanceOf(RefreshTokenReuseException.class);

        verify(refreshTokenStorePort).revokeFamily(FAMILY_ID);
    }
}
