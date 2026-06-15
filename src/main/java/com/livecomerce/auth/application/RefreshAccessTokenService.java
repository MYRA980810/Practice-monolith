package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.RefreshAccessTokenUseCase;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.RefreshTokenStorePort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.domain.RefreshToken;
import com.livecomerce.auth.infrastructure.security.JwtProperties;
import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshAccessTokenService implements RefreshAccessTokenUseCase {

    private final RefreshTokenStorePort refreshTokenStorePort;
    private final LoadUserPort loadUserPort;
    private final TokenGeneratorPort tokenGeneratorPort;
    private final RefreshTokenHasher refreshTokenHasher;
    private final JwtProperties jwtProperties;

    @Override
    // noRollbackFor is required here: RefreshTokenReuseException is a RuntimeException
    // which would normally trigger a rollback — undoing revokeFamily and defeating theft detection.
    // We WANT the family revocation to commit even as the exception propagates to return 401.
    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public RefreshResult refresh(String rawRefreshToken) {
        var hash = refreshTokenHasher.hash(rawRefreshToken);

        var existing = refreshTokenStorePort.loadByTokenHash(hash)
                .orElseThrow(RefreshTokenInvalidException::new);

        // Reuse detection: token was already consumed
        if (existing.isConsumed()) {
            refreshTokenStorePort.revokeFamily(existing.getFamilyId());
            throw new RefreshTokenReuseException();
        }

        // Validate active state (not expired, not revoked)
        if (!existing.isActive(Instant.now())) {
            throw new RefreshTokenInvalidException();
        }

        // Issue new token in the same family
        var newRawToken = refreshTokenHasher.generateRawToken();
        var newHash = refreshTokenHasher.hash(newRawToken);
        var newToken = RefreshToken.issue(existing.getUserId(), existing.getFamilyId(), newHash,
                jwtProperties.refreshExpirationMs());

        var saved = refreshTokenStorePort.save(newToken);

        // Rotate the old token (mark used + revoked + replaced_by)
        existing.rotate(saved.getId());
        refreshTokenStorePort.save(existing);

        // Generate new access token
        var user = loadUserPort.loadById(existing.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found: " + existing.getUserId()));

        var newAccessToken = tokenGeneratorPort.generate(user);

        return new RefreshResult(newAccessToken, newRawToken);
    }
}
