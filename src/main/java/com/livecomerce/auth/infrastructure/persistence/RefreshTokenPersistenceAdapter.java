package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.application.port.out.IssueRefreshTokenPort;
import com.livecomerce.auth.application.port.out.RefreshTokenStorePort;
import com.livecomerce.auth.domain.RefreshToken;
import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class RefreshTokenPersistenceAdapter implements RefreshTokenStorePort, IssueRefreshTokenPort {

    private final RefreshTokenJpaRepository repository;
    private final RefreshTokenHasher refreshTokenHasher;

    @Value("${jwt.refresh-expiration-ms:2592000000}")
    private long refreshExpirationMs;

    @Override
    public RefreshToken save(RefreshToken token) {
        return repository.save(token);
    }

    @Override
    public Optional<RefreshToken> loadByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash);
    }

    @Override
    public void revokeFamily(UUID familyId) {
        repository.revokeAllByFamilyId(familyId);
    }

    @Override
    public String issueForUser(UUID userId) {
        var rawToken = refreshTokenHasher.generateRawToken();
        var hash = refreshTokenHasher.hash(rawToken);
        var familyId = UUID.randomUUID();
        var token = RefreshToken.issue(userId, familyId, hash, refreshExpirationMs);
        repository.save(token);
        return rawToken;
    }
}
