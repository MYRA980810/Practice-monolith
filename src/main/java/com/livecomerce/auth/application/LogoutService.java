package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.LogoutUseCase;
import com.livecomerce.auth.application.port.out.RefreshTokenStorePort;
import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LogoutService implements LogoutUseCase {

    private final RefreshTokenStorePort refreshTokenStorePort;
    private final RefreshTokenHasher refreshTokenHasher;

    @Override
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        var hash = refreshTokenHasher.hash(rawRefreshToken);
        refreshTokenStorePort.loadByTokenHash(hash)
                .ifPresent(token -> refreshTokenStorePort.revokeFamily(token.getFamilyId()));
    }
}
