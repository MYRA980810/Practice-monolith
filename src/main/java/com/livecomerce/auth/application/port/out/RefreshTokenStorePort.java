package com.livecomerce.auth.application.port.out;

import com.livecomerce.auth.domain.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStorePort {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> loadByTokenHash(String tokenHash);

    /**
     * Loads the token by hash and takes a pessimistic write lock on its row until the
     * surrounding transaction ends, serializing concurrent refreshes of the same token.
     */
    Optional<RefreshToken> loadByTokenHashForUpdate(String tokenHash);

    Optional<RefreshToken> loadById(UUID id);

    void revokeFamily(UUID familyId);

    void revokeAllForUserExcept(UUID userId, UUID exceptFamilyId);
}
