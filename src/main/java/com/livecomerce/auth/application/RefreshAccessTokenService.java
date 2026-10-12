package com.livecomerce.auth.application;

import com.livecomerce.auth.application.port.in.RefreshAccessTokenUseCase;
import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.auth.application.port.out.RefreshTokenStorePort;
import com.livecomerce.auth.application.port.out.TokenGeneratorPort;
import com.livecomerce.auth.domain.RefreshToken;
import com.livecomerce.auth.infrastructure.security.JwtProperties;
import com.livecomerce.auth.infrastructure.security.RefreshTokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
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
    private final Clock clock;

    @Override
    // noRollbackFor is required here: RefreshTokenReuseException is a RuntimeException
    // which would normally trigger a rollback — undoing revokeFamily and defeating theft detection.
    // We WANT the family revocation to commit even as the exception propagates to return 401.
    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public RefreshResult refresh(String rawRefreshToken) {
        var hash = refreshTokenHasher.hash(rawRefreshToken);

        var existing = loadLocked(hash);
        var now = Instant.now(clock);

        // Reuse detection: token was already consumed
        if (existing.isConsumed()) {
            if (isConcurrentRefresh(existing, now)) {
                // Legit race, not theft: nothing is revoked or issued; the client retries
                // with the cookie set by the winning request.
                throw new RefreshTokenRetryException();
            }
            refreshTokenStorePort.revokeFamily(existing.getFamilyId());
            throw new RefreshTokenReuseException();
        }

        // Validate active state (not expired, not revoked)
        if (!existing.isActive(now)) {
            throw new RefreshTokenInvalidException();
        }

        // Issue new token in the same family
        var newRawToken = refreshTokenHasher.generateRawToken();
        var newHash = refreshTokenHasher.hash(newRawToken);
        var newToken = RefreshToken.issue(existing.getUserId(), existing.getFamilyId(), newHash,
                jwtProperties.refreshExpirationMs());

        var saved = refreshTokenStorePort.save(newToken);

        // Rotate the old token (mark used + revoked + replaced_by)
        existing.rotate(saved.getId(), now);
        refreshTokenStorePort.save(existing);

        // Generate new access token
        var user = loadUserPort.loadById(existing.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found: " + existing.getUserId()));

        var newAccessToken = tokenGeneratorPort.generate(user);

        return new RefreshResult(newAccessToken, newRawToken);
    }

    /**
     * Row lock serializes concurrent refreshes of the same token (e.g. two tabs sharing the cookie):
     * the loser blocks here and then sees the token as already consumed by the winner.
     * The wait is bounded; on timeout the lock holder is still rotating the token, so the client
     * is told to retry with the current cookie instead of tying up a connection.
     */
    private RefreshToken loadLocked(String hash) {
        try {
            return refreshTokenStorePort.loadByTokenHashForUpdate(hash)
                    .orElseThrow(RefreshTokenInvalidException::new);
        } catch (PessimisticLockingFailureException e) {
            throw new RefreshTokenRetryException(e);
        }
    }

    /**
     * A consumed token presented again is a concurrent refresh (not theft) only if it was rotated
     * within the grace window AND its successor is still active. The successor check denies grace
     * after logout or password change, both of which revoke the successor.
     */
    private boolean isConcurrentRefresh(RefreshToken consumed, Instant now) {
        var graceDeadline = consumed.getUsedAt().plusMillis(jwtProperties.refreshReuseGraceMs());
        if (now.isAfter(graceDeadline) || consumed.getReplacedBy() == null) {
            return false;
        }
        return refreshTokenStorePort.loadById(consumed.getReplacedBy())
                .map(successor -> successor.isActive(now))
                .orElse(false);
    }
}
