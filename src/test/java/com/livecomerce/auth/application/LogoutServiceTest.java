package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.out.RefreshTokenStorePort;
import com.livecomerce.auth.domain.RefreshToken;
import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock RefreshTokenStorePort refreshTokenStorePort;
    @Mock RefreshTokenHasher refreshTokenHasher;

    @InjectMocks LogoutService service;

    private static final UUID USER_ID   = UUID.randomUUID();
    private static final UUID FAMILY_ID = UUID.randomUUID();
    private static final String RAW_TOKEN  = "raw-refresh-token";
    private static final String TOKEN_HASH = "hashed-token";

    @Test
    void logout_withValidToken_revokesFamilyAndReturnsTrue() {
        var token = RefreshToken.issue(USER_ID, FAMILY_ID, TOKEN_HASH, 30L * 24 * 60 * 60 * 1000);

        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(token));

        service.logout(RAW_TOKEN);

        verify(refreshTokenStorePort).revokeFamily(FAMILY_ID);
    }

    @Test
    void logout_withNullToken_doesNothing() {
        service.logout(null);

        verify(refreshTokenStorePort, never()).loadByTokenHash(any());
        verify(refreshTokenStorePort, never()).revokeFamily(any());
    }

    @Test
    void logout_withTokenNotFound_doesNothing() {
        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStorePort.loadByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        service.logout(RAW_TOKEN);

        verify(refreshTokenStorePort, never()).revokeFamily(any());
    }
}
