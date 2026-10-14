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
    private final RowLockTimeout rowLockTimeout;

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
    public Optional<RefreshToken> loadByTokenHashForUpdate(String tokenHash) {
        // Without a bound, waiters block indefinitely while each holds a pooled connection.
        rowLockTimeout.applyToCurrentTransaction();
        return repository.findForUpdateByTokenHash(tokenHash);
    }

    @Override
    public Optional<RefreshToken> loadById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public void revokeFamily(UUID familyId) {
        repository.revokeAllByFamilyId(familyId);
    }

    @Override
    public void revokeAllForUserExcept(UUID userId, UUID exceptFamilyId) {
        repository.revokeAllByUserIdExceptFamily(userId, exceptFamilyId);
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
